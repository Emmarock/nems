package com.nitelestate.notification;

import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.notification.dto.UserNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserNotificationRepository userNotificationRepository;

    /**
     * Creates a personal in-app alert for one user. Silently does nothing when userId is null —
     * e.g. a host resident who has no portal login yet has nowhere for this to show up; the SMS
     * dispatched alongside this (see VisitorService/WorkerService) still reaches them regardless.
     */
    @Transactional
    public void notify(Long userId, String title, String message) {
        if (userId == null) {
            return;
        }
        UserNotification n = new UserNotification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(message);
        userNotificationRepository.save(n);
    }

    public PageResponse<UserNotificationResponse> search(Long userId, Pageable pageable) {
        return PageResponse.of(userNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable),
                UserNotificationResponse::from);
    }

    public long unreadCount(Long userId) {
        return userNotificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        UserNotification n = get(id, userId);
        n.setRead(true);
        userNotificationRepository.save(n);
    }

    @Transactional
    public void markUnread(Long id, Long userId) {
        UserNotification n = get(id, userId);
        n.setRead(false);
        userNotificationRepository.save(n);
    }

    /** Not found rather than forbidden if it belongs to someone else — doesn't confirm another user's notification even exists. */
    private UserNotification get(Long id, Long userId) {
        UserNotification n = userNotificationRepository.findById(id).orElseThrow(() -> NotFoundException.of("UserNotification", id));
        if (!n.getUserId().equals(userId)) {
            throw NotFoundException.of("UserNotification", id);
        }
        return n;
    }
}
