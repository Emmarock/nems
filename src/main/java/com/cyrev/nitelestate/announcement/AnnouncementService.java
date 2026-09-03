package com.cyrev.nitelestate.announcement;

import com.cyrev.nitelestate.announcement.dto.AnnouncementRequest;
import com.cyrev.nitelestate.announcement.dto.AnnouncementResponse;
import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationService;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.resident.ResidentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final ResidentRepository residentRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public AnnouncementResponse create(Long createdByUserId, AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.title());
        announcement.setMessage(request.message());
        announcement.setCreatedByUserId(createdByUserId);
        announcement.setChannels(request.channels());
        announcement = announcementRepository.save(announcement);

        broadcast(announcement);

        auditService.record("Announcement", announcement.getId(), "CREATE", announcement.getTitle());
        return AnnouncementResponse.from(announcement, false);
    }

    public PageResponse<AnnouncementResponse> search(Long currentUserId, Pageable pageable) {
        Set<Long> readIds = new HashSet<>(announcementReadRepository.findAnnouncementIdsByUserId(currentUserId));
        return PageResponse.of(announcementRepository.findAll(pageable)
                .map(a -> AnnouncementResponse.from(a, readIds.contains(a.getId()))));
    }

    public long unreadCount(Long currentUserId) {
        long total = announcementRepository.count();
        long read = announcementReadRepository.countByUserId(currentUserId);
        return Math.max(0, total - read);
    }

    @Transactional
    public void markRead(Long announcementId, Long userId) {
        if (!announcementRepository.existsById(announcementId)) {
            throw NotFoundException.of("Announcement", announcementId);
        }
        if (announcementReadRepository.findByAnnouncementIdAndUserId(announcementId, userId).isEmpty()) {
            AnnouncementRead read = new AnnouncementRead();
            read.setAnnouncementId(announcementId);
            read.setUserId(userId);
            announcementReadRepository.save(read);
        }
    }

    @Transactional
    public void markUnread(Long announcementId, Long userId) {
        if (!announcementRepository.existsById(announcementId)) {
            throw NotFoundException.of("Announcement", announcementId);
        }
        announcementReadRepository.deleteByAnnouncementIdAndUserId(announcementId, userId);
    }

    private void broadcast(Announcement announcement) {
        if (announcement.getChannels().stream().noneMatch(c -> c != NotificationChannel.PORTAL)) {
            return;
        }
        List<Resident> residents = residentRepository.findAll().stream()
                .filter(r -> r.getStatus() == ResidentStatus.ACTIVE).toList();

        for (Resident resident : residents) {
            if (announcement.getChannels().contains(NotificationChannel.SMS)) {
                notificationService.dispatch(NotificationChannel.SMS, resident.getPhone(), announcement.getMessage());
            }
            if (announcement.getChannels().contains(NotificationChannel.WHATSAPP)) {
                notificationService.dispatch(NotificationChannel.WHATSAPP, resident.getPhone(), announcement.getMessage());
            }
            if (announcement.getChannels().contains(NotificationChannel.EMAIL) && resident.getEmail() != null) {
                notificationService.dispatch(NotificationChannel.EMAIL, resident.getEmail(), announcement.getMessage());
            }
        }
    }
}
