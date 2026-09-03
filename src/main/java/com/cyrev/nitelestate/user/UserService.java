package com.cyrev.nitelestate.user;

import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.ConflictException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.user.dto.BulkCreateResidentUsersResponse;
import com.cyrev.nitelestate.user.dto.UserCreateRequest;
import com.cyrev.nitelestate.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    /** Excludes 0/O and 1/I/l - this gets printed on handout slips, so ambiguous glyphs are a real problem. */
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 10;
    private static final String SYNTHETIC_EMAIL_DOMAIN = "@nitelestate.local";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user with email " + request.email() + " already exists");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setResidentId(request.residentId());
        user.setStatus(UserStatus.ACTIVE);
        return UserResponse.from(userRepository.save(user));
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

        List<BulkCreateResidentUsersResponse.CreatedAccount> created = new ArrayList<>();
        for (Resident resident : residentRepository.findAllById(toCreate)) {
            String email = buildLoginEmail(resident, usedEmailsLower);
            String temporaryPassword = generateTemporaryPassword();

            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
            user.setFullName(resident.getFullName());
            user.setRole(Role.RESIDENT);
            user.setResidentId(resident.getId());
            user.setStatus(UserStatus.ACTIVE);
            user.setMustChangePassword(true);
            userRepository.save(user);

            created.add(new BulkCreateResidentUsersResponse.CreatedAccount(
                    resident.getId(), resident.getFullName(), email, temporaryPassword));
        }

        return new BulkCreateResidentUsersResponse(created, targetIds.size() - toCreate.size());
    }

    /** Prefers the resident's own phone number (memorable) over an arbitrary ID, falling back
     * when the phone is missing/"UNKNOWN" or would collide with an email already in use. */
    private String buildLoginEmail(Resident resident, Set<String> usedEmailsLower) {
        String candidate = null;
        String phone = resident.getPhone();
        if (phone != null && !phone.equalsIgnoreCase("UNKNOWN")) {
            String digits = phone.replaceAll("\\D", "");
            if (!digits.isEmpty()) {
                candidate = digits + SYNTHETIC_EMAIL_DOMAIN;
            }
        }
        if (candidate == null || usedEmailsLower.contains(candidate.toLowerCase())) {
            candidate = "resident" + resident.getId() + SYNTHETIC_EMAIL_DOMAIN;
        }
        usedEmailsLower.add(candidate.toLowerCase());
        return candidate;
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    private User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }
}
