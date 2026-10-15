package com.nitelestate.notification;

import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.search.Paging;
import com.nitelestate.notification.dto.UserNotificationResponse;
import com.nitelestate.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A signed-in user's own personal, in-app alerts — distinct from /announcements, which is
 * estate-wide and every resident sees the same set. Always scoped to the caller; no role
 * restriction, since any signed-in user (resident, tenant, or staff) can have these.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService userNotificationService;
    private final CurrentUser currentUser;

    @GetMapping
    public PageResponse<UserNotificationResponse> findAll(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return userNotificationService.search(currentUser.userId(), Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", userNotificationService.unreadCount(currentUser.userId()));
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        userNotificationService.markRead(id, currentUser.userId());
    }

    @PostMapping("/{id}/unread")
    public void markUnread(@PathVariable Long id) {
        userNotificationService.markUnread(id, currentUser.userId());
    }
}
