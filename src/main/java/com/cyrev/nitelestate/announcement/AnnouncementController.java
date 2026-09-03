package com.cyrev.nitelestate.announcement;

import com.cyrev.nitelestate.announcement.dto.AnnouncementRequest;
import com.cyrev.nitelestate.announcement.dto.AnnouncementResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY')")
    public AnnouncementResponse create(@Valid @RequestBody AnnouncementRequest request) {
        return announcementService.create(currentUser.userId(), request);
    }

    @GetMapping
    public PageResponse<AnnouncementResponse> findAll(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return announcementService.search(currentUser.userId(), Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", announcementService.unreadCount(currentUser.userId()));
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        announcementService.markRead(id, currentUser.userId());
    }

    @PostMapping("/{id}/unread")
    public void markUnread(@PathVariable Long id) {
        announcementService.markUnread(id, currentUser.userId());
    }
}
