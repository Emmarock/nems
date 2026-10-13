package com.nitelestate.notification;

import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.search.Paging;
import com.nitelestate.common.search.Specs;
import com.nitelestate.notification.dto.NotificationLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only audit trail of every SMS/WhatsApp/email dispatch the app has attempted — lets an
 * admin check "did this actually go out" (e.g. a resident reporting they never got their
 * claim-account code) without needing server log/shell access. message is already redacted at
 * write time (see NotificationService.handle / Redaction) — nothing further to scrub here.
 */
@RestController
@RequestMapping("/api/v1/notification-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class NotificationLogController {

    private final NotificationLogRepository notificationLogRepository;

    @GetMapping
    public PageResponse<NotificationLogResponse> search(
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Specification<NotificationLog> spec = Specification.<NotificationLog>where(Specs.eq(channel, "channel"))
                .and(Specs.eq(status, "status"))
                .and(Specs.contains(q, "recipient"));
        Page<NotificationLog> result = notificationLogRepository.findAll(spec, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, NotificationLogResponse::from);
    }
}
