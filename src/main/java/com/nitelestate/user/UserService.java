package com.nitelestate.user;

import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.common.exception.ConflictException;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.common.search.Specs;
import com.nitelestate.notification.MessageTemplates;
import com.nitelestate.notification.NotificationChannel;
import com.nitelestate.notification.NotificationService;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.user.dto.BulkCreateResidentUsersResponse;
import com.nitelestate.user.dto.UserCreateRequest;
import com.nitelestate.user.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    /** Excludes 0/O and 1/I/l - this gets printed on handout slips, so ambiguous glyphs are a real problem. */
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 10;
    private static final String SYNTHETIC_EMAIL_DOMAIN = "@nitelestate.local";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RESET_TOKEN_VALIDITY_HOURS = 24;
    private static final int OTP_VALIDITY_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final GeoLocationService geoLocationService;
    private final String appBaseUrl;

    public UserService(UserRepository userRepository, ResidentRepository residentRepository,
                        PasswordEncoder passwordEncoder, NotificationService notificationService,
                        GeoLocationService geoLocationService, @Value("${nitel.app.base-url}") String appBaseUrl) {
        this.userRepository = userRepository;
        this.residentRepository = residentRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.geoLocationService = geoLocationService;
        this.appBaseUrl = appBaseUrl;
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user with email " + request.email() + " already exists");
        }
        String normalizedPhone = PhoneNumbers.normalize(request.phone());
        if (normalizedPhone != null && userRepository.existsByPhone(normalizedPhone)) {
            throw new ConflictException("A user with phone " + request.phone() + " already exists");
        }
        if (request.role() == Role.RESIDENT) {
            if (request.residentId() == null) {
                throw new BadRequestException("A resident-role account must be linked to a resident");
            }
            if (!residentRepository.existsById(request.residentId())) {
                throw new BadRequestException("No resident found with id " + request.residentId());
            }
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPhone(normalizedPhone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setResidentId(request.residentId());
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        notifyNewAccount(user, request.password(), user.isMustChangePassword());
        return UserResponse.from(user);
    }

    public PageResponse<UserResponse> search(String q, Pageable pageable) {
        return PageResponse.of(userRepository.findAll(Specs.contains(q, "fullName", "email"), pageable), UserResponse::from);
    }

    public UserResponse findById(Long id) {
        return UserResponse.from(get(id));
    }

    @Transactional
    public UserResponse setStatus(Long id, UserStatus status) {
        User user = get(id);
        user.setStatus(status);
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Self-service: any authenticated user changing their own password must prove they know the
     * current one (a temporary password counts - it's still "the current one"). Always clears
     * mustChangePassword, since choosing your own password is exactly what that flag is waiting for.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = get(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    /**
     * Admin reset (e.g. a resident/staff member forgot their password) — no current-password
     * check. Sets mustChangePassword since the holder didn't choose this password either.
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = get(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
    }

    /**
     * Consumes a "set your password" link token (see issueResetToken / the account-provisioned
     * email) — no prior login needed, the token itself is the credential. Rejects an unknown,
     * already-used, or expired token with the same message either way, so there's nothing to
     * learn from the failure. Clears the token on success so the link can't be replayed.
     */
    @Transactional
    public void resetPasswordWithToken(String rawToken, String newPassword) {
        User user = userRepository.findByResetTokenHash(hashToken(rawToken))
                .filter(u -> u.getResetTokenExpiresAt() != null && u.getResetTokenExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new BadRequestException("This link is invalid or has expired — ask for a new one"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setResetTokenHash(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    /**
     * Creates a RESIDENT-role login for every given resident who doesn't already have one (or
     * every resident with no account at all, if residentIds is empty) — the mechanism behind
     * "share credentials with existing residents" regardless of how they end up handed out.
     * Each gets a random temporary password and mustChangePassword=true; the plaintext password
     * is only ever returned here, in this one response, for the admin to export/print.
     */
    @Transactional
    public BulkCreateResidentUsersResponse bulkCreateResidentAccounts(List<Long> residentIds) {
        List<Long> targetIds = (residentIds == null || residentIds.isEmpty())
                ? residentRepository.findAllIds()
                : residentIds;

        Set<Long> alreadyLinked = new HashSet<>(userRepository.findAllLinkedResidentIds());
        List<Long> toCreate = targetIds.stream().distinct().filter(id -> !alreadyLinked.contains(id)).toList();

        Set<String> usedEmailsLower = new HashSet<>();
        userRepository.findAllEmails().forEach(email -> usedEmailsLower.add(email.toLowerCase()));
        Set<String> usedPhones = new HashSet<>(userRepository.findAllPhones());

        List<BulkCreateResidentUsersResponse.CreatedAccount> created = new ArrayList<>();
        for (Resident resident : residentRepository.findAllById(toCreate)) {
            String email = buildLoginEmail(resident, usedEmailsLower);
            String phone = resolveLoginPhone(resident, usedPhones);
            String temporaryPassword = generateTemporaryPassword();

            User user = new User();
            user.setEmail(email);
            user.setPhone(phone);
            user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
            user.setFullName(resident.getFullName());
            user.setRole(Role.RESIDENT);
            user.setResidentId(resident.getId());
            user.setStatus(UserStatus.ACTIVE);
            user.setMustChangePassword(true);
            userRepository.save(user);
            notifyNewAccount(user, temporaryPassword, true);

            created.add(new BulkCreateResidentUsersResponse.CreatedAccount(
                    resident.getId(), resident.getFullName(), email, phone, temporaryPassword));
        }

        return new BulkCreateResidentUsersResponse(created, targetIds.size() - toCreate.size());
    }

    /** Real phone (not "UNKNOWN"), normalized, skipped if it would collide with another account's. */
    private String resolveLoginPhone(Resident resident, Set<String> usedPhones) {
        String normalized = PhoneNumbers.normalize(resident.getPhone());
        if (normalized == null || usedPhones.contains(normalized)) {
            return null;
        }
        usedPhones.add(normalized);
        return normalized;
    }

    /**
     * Prefers the resident's own real email (recognizable, and lets notifyNewAccount actually
     * deliver the welcome message by EMAIL instead of just SMS/WhatsApp), then their phone number,
     * falling back to an arbitrary synthetic id only when neither is available or both collide
     * with a login already in use.
     */
    private String buildLoginEmail(Resident resident, Set<String> usedEmailsLower) {
        String candidate = null;
        if (resident.getEmail() != null && !resident.getEmail().isBlank()
                && !usedEmailsLower.contains(resident.getEmail().toLowerCase())) {
            candidate = resident.getEmail();
        }
        if (candidate == null) {
            String normalizedPhone = PhoneNumbers.normalize(resident.getPhone());
            if (normalizedPhone != null) {
                candidate = normalizedPhone.substring(1) + SYNTHETIC_EMAIL_DOMAIN; // drop the leading "+"
            }
        }
        if (candidate == null || usedEmailsLower.contains(candidate.toLowerCase())) {
            candidate = "resident" + resident.getId() + SYNTHETIC_EMAIL_DOMAIN;
        }
        usedEmailsLower.add(candidate.toLowerCase());
        return candidate;
    }

    /**
     * Notifies a newly-created account holder of their login and password, on every channel
     * they actually have a real contact method for. Skips email for the synthetic
     * {@code @nitelestate.local} placeholder (see buildLoginEmail) - that's a login key, not a
     * real inbox, so mailing it would just bounce. Phone is stored as a real E.164 number (see
     * PhoneNumbers) so it's already dispatchable as-is, Nigerian or international.
     */
    private void notifyNewAccount(User user, String password, boolean mustChangePassword) {
        String loginId = isRealEmail(user.getEmail())
                ? user.getEmail()
                : (user.getPhone() != null ? user.getPhone() : user.getEmail());
        // A forced-reset account gets a one-click "set your password" link (no need to retype the
        // temp password); an admin-chosen, ready-to-use password just links to the ordinary login.
        String actionUrl = mustChangePassword
                ? appBaseUrl + "/reset-password?token=" + issueResetToken(user)
                : appBaseUrl + "/login";
        String message = MessageTemplates.accountProvisioned(loginId, password, mustChangePassword, actionUrl);

        if (isRealEmail(user.getEmail())) {
            notificationService.dispatch(NotificationChannel.EMAIL, user.getEmail(), message);
        }
        if (user.getPhone() != null) {
            notificationService.dispatch(NotificationChannel.SMS, user.getPhone(), message);
            notificationService.dispatch(NotificationChannel.WHATSAPP, user.getPhone(), message);
        }
    }

    /**
     * "You just signed in" email, fired on every successful login (see AuthController.login).
     * For a resident-linked account, targets Resident.email — their real contact address -
     * rather than the login identifier, which is usually a synthetic placeholder (see
     * buildLoginEmail/resolveLoginPhone) that would otherwise never pass isRealEmail and silently
     * never notify anyone. Does nothing if no real address is on file either way.
     *
     * Runs off the request thread (same executor NotificationService itself uses) because the
     * geo-IP lookup is a real outbound HTTP call - it must never add latency, let alone a
     * failure mode, to the login request itself.
     */
    @Async("notificationExecutor")
    public void notifyLogin(Long userId, String ipAddress, String userAgent) {
        User user = get(userId);
        String targetEmail = resolveNotificationEmail(user);
        if (targetEmail == null) {
            return;
        }
        String location = geoLocationService.lookup(ipAddress);
        String message = MessageTemplates.loginAlert(user.getFullName(), UserAgents.describe(userAgent), ipAddress, location);
        notificationService.dispatch(NotificationChannel.EMAIL, targetEmail, message);
    }

    /**
     * Step 1 of self-service "I'm already a resident, give me access" recovery. Knowing a phone
     * number alone isn't proof of anything - it can be guessed, leaked, or publicly known - so
     * this only ever sends a one-time code to that number over SMS, a channel only the real
     * holder of the phone can read. No email goes out here, and no link is issued until
     * verifyClaimAccount confirms the code.
     *
     * Reports an unmatched phone back to the caller rather than staying silent - a deliberate
     * trade: this does let someone probe which phone numbers are on file, but a clear "that
     * number isn't registered" beats a resident assuming the system is broken when they mistype
     * theirs. verifyClaimAccount stays silent on a wrong/expired code, since by that point
     * there's nothing left to protect by hiding it either way.
     */
    @Transactional
    public void startClaimAccount(String phone) {
        BadRequestException notFound = new BadRequestException(
                "We couldn't find a resident with that phone number, ensure it is the number you register with on the resident group — check it and try again");
        String normalizedPhone = PhoneNumbers.normalize(phone);
        if (normalizedPhone == null) {
            throw notFound;
        }
        Resident resident = findResidentByPhone(normalizedPhone);
        if (resident == null) {
            throw notFound;
        }
        String code = generateOtp();
        resident.setClaimOtpHash(hashToken(code));
        resident.setClaimOtpExpiresAt(Instant.now().plus(OTP_VALIDITY_MINUTES, ChronoUnit.MINUTES));
        resident.setClaimOtpAttempts(0);
        residentRepository.save(resident);
        String message = MessageTemplates.claimOtp(code);
        // transactional=true - BulkSmsNigeriaSender routes this through the provider's OTP
        // gateway instead of the bulk one, which rejects OTP-shaped content outright.
        notificationService.dispatch(NotificationChannel.SMS, normalizedPhone, message, true);
    }

    /**
     * Step 2 - the code just proved the caller controls the phone on the resident record, so the
     * supplied email is now trusted as the delivery address (updating it on file if it differs)
     * and a reset link goes out exactly as onboarding does. Unlike startClaimAccount, a wrong
     * code is reported back (so the UI can let the caller retry) rather than swallowed - by this
     * point the caller has already proven they received the real code at least once, so there's
     * nothing left to protect by staying silent; repeated wrong guesses are capped by
     * claimOtpAttempts instead.
     */
    @Transactional
    public void verifyClaimAccount(String phone, String code, String email) {
        String normalizedPhone = PhoneNumbers.normalize(phone);
        BadRequestException invalidCode = new BadRequestException("Invalid or expired code — ask for a new one");
        if (normalizedPhone == null || code == null || code.isBlank() || email == null || email.isBlank()) {
            throw invalidCode;
        }
        Resident resident = findResidentByPhone(normalizedPhone);
        if (resident == null || resident.getClaimOtpHash() == null
                || resident.getClaimOtpExpiresAt() == null || resident.getClaimOtpExpiresAt().isBefore(Instant.now())
                || resident.getClaimOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            throw invalidCode;
        }
        if (!resident.getClaimOtpHash().equals(hashToken(code.trim()))) {
            residentRepository.incrementClaimOtpAttempts(resident.getId());
            throw invalidCode;
        }

        resident.setClaimOtpHash(null);
        resident.setClaimOtpExpiresAt(null);
        resident.setClaimOtpAttempts(0);
        resident.setEmail(email.trim());
        residentRepository.save(resident);
        notifyLoginDetails(resident);
    }

    /**
     * Emails login access details to a resident whose email was just captured — by the
     * claim-account flow above, by an admin editing their record, or by the resident adding it
     * to their own profile (see ResidentService.update/updateSelf). Finds their login if one
     * already exists or provisions one (same as claimAccount), then sends a one-time
     * "set your password" link rather than any password, so this is safe to call regardless of
     * whether the account already has a real password set. No-ops if the resident still has no
     * email - defensive, since every caller is expected to check "email was just added" first.
     */
    @Transactional
    public void notifyLoginDetails(Resident resident) {
        if (resident.getEmail() == null || resident.getEmail().isBlank()) {
            return;
        }
        User user = userRepository.findByResidentId(resident.getId()).orElseGet(() -> provisionResidentLogin(resident));
        String resetUrl = appBaseUrl + "/reset-password?token=" + issueResetToken(user);
        notificationService.dispatch(NotificationChannel.EMAIL, resident.getEmail(),
                MessageTemplates.accountClaimed(resident.getFullName(), resetUrl));
    }

    /** Zero-padded so it's always OTP_LENGTH digits (e.g. "003492"), never a shorter number. */
    private String generateOtp() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private Resident findResidentByPhone(String normalizedPhone) {
        for (ResidentRepository.ResidentPhoneRow row : residentRepository.findAllIdsAndPhones()) {
            if (normalizedPhone.equals(PhoneNumbers.normalize(row.getPhone()))) {
                return residentRepository.findById(row.getId()).orElse(null);
            }
        }
        return null;
    }

    /**
     * Mirrors bulkCreateResidentAccounts for a single resident claiming access for the first
     * time - random password is never revealed anywhere since claimAccount only ever sends a
     * reset link, never a password.
     */
    private User provisionResidentLogin(Resident resident) {
        boolean emailAvailable = resident.getEmail() != null && !resident.getEmail().isBlank()
                && !userRepository.existsByEmailIgnoreCase(resident.getEmail());
        String normalizedPhone = PhoneNumbers.normalize(resident.getPhone());
        boolean phoneAvailable = normalizedPhone != null && !userRepository.existsByPhone(normalizedPhone);

        User user = new User();
        user.setEmail(emailAvailable ? resident.getEmail() : "resident" + resident.getId() + SYNTHETIC_EMAIL_DOMAIN);
        user.setPhone(phoneAvailable ? normalizedPhone : null);
        user.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
        user.setFullName(resident.getFullName());
        user.setRole(Role.RESIDENT);
        user.setResidentId(resident.getId());
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(true);
        return userRepository.save(user);
    }

    private String resolveNotificationEmail(User user) {
        if (user.getResidentId() != null) {
            return residentRepository.findById(user.getResidentId())
                    .map(Resident::getEmail)
                    .filter(email -> email != null && !email.isBlank())
                    .orElse(null);
        }
        return isRealEmail(user.getEmail()) ? user.getEmail() : null;
    }

    private boolean isRealEmail(String email) {
        return email != null && !email.toLowerCase().endsWith(SYNTHETIC_EMAIL_DOMAIN);
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    /** Mints a fresh random token for this user's "set your password" link, storing only its hash. */
    private String issueResetToken(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        user.setResetTokenHash(hashToken(rawToken));
        user.setResetTokenExpiresAt(Instant.now().plus(RESET_TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS));
        userRepository.save(user);
        return rawToken;
    }

    /** SHA-256 is fine here (unlike passwordHash's bcrypt) — the token is already 256 bits of random entropy, not a human-chosen secret to protect against brute force. */
    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available on the JVM", e);
        }
    }

    private User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }
}
