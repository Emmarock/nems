package com.nitelestate.user;

import com.nitelestate.notification.NotificationChannel;
import com.nitelestate.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Emails each estate office about a task for it, e.g. a new payment receipt to review. Per office,
 * the recipients are, in order of preference:
 * <ol>
 *   <li>the personal email set on the office's account(s) (Office profile);</li>
 *   <li>otherwise the email of the resident the super admin assigned to the office (OfficeHolder);</li>
 *   <li>otherwise the office account's login email, when it's a real address.</li>
 * </ol>
 * Each address is emailed once, even if it serves several offices.
 */
@Service
public class OfficeNotificationService {

    private final UserRepository userRepository;
    private final OfficeHolderRepository officeHolderRepository;
    private final ResidentRepository residentRepository;
    private final NotificationService notificationService;
    private final String appBaseUrl;

    public OfficeNotificationService(UserRepository userRepository, OfficeHolderRepository officeHolderRepository,
                                     ResidentRepository residentRepository, NotificationService notificationService,
                                     @Value("${nitel.app.base-url}") String appBaseUrl) {
        this.userRepository = userRepository;
        this.officeHolderRepository = officeHolderRepository;
        this.residentRepository = residentRepository;
        this.notificationService = notificationService;
        this.appBaseUrl = appBaseUrl;
    }

    /** An absolute link into the app, e.g. appLink("/payments?review=12"). */
    public String appLink(String path) {
        return appBaseUrl + path;
    }

    public void notifyOffices(Collection<Role> offices, String subject, String message) {
        Set<String> sent = new LinkedHashSet<>();
        for (String to : recipients(offices)) {
            if (sent.add(to.toLowerCase(Locale.ROOT))) {
                notificationService.dispatch(NotificationChannel.EMAIL, to, message, subject);
            }
        }
    }

    List<String> recipients(Collection<Role> offices) {
        Map<Role, List<User>> accounts = userRepository.findAllByRoleInAndStatus(offices, UserStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(User::getRole));
        Map<Role, OfficeHolder> holders = officeHolderRepository.findAllByRoleIn(offices).stream()
                .collect(Collectors.toMap(OfficeHolder::getRole, h -> h));
        List<String> out = new ArrayList<>();
        for (Role office : offices) {
            List<User> users = accounts.getOrDefault(office, List.of());
            List<String> personal = users.stream().map(User::getPersonalEmail)
                    .filter(e -> e != null && !e.isBlank()).map(String::trim).toList();
            if (!personal.isEmpty()) {
                out.addAll(personal);
                continue;
            }
            String holderEmail = Optional.ofNullable(holders.get(office))
                    .flatMap(h -> residentRepository.findById(h.getResidentId()))
                    .map(Resident::getEmail)
                    .filter(e -> e != null && !e.isBlank() && UserService.isRealEmail(e))
                    .map(String::trim)
                    .orElse(null);
            if (holderEmail != null) {
                out.add(holderEmail);
                continue;
            }
            users.stream().map(User::getEmail).filter(UserService::isRealEmail).forEach(out::add);
        }
        return out;
    }
}
