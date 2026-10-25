package com.nitelestate.announcement;

import com.nitelestate.announcement.dto.AnnouncementRequest;
import com.nitelestate.announcement.dto.AnnouncementResponse;
import com.nitelestate.audit.AuditService;
import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.notification.NotificationAttachment;
import com.nitelestate.notification.NotificationChannel;
import com.nitelestate.notification.NotificationService;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.resident.ResidentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
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

    /** ~1.5MB of base64 — same cap as Worker/Visitor/HouseholdMember photos. */
    private static final int MAX_PHOTO_LENGTH = 2_000_000;

    @Transactional
    public AnnouncementResponse create(Long createdByUserId, AnnouncementRequest request) {
        if (request.photo() != null) {
            if (request.photo().length() > MAX_PHOTO_LENGTH) {
                throw new BadRequestException("Photo is too large — please use a smaller image");
            }
            if (request.channels().contains(NotificationChannel.SMS)) {
                throw new BadRequestException("An announcement with an image can't be sent by SMS — remove the image or deselect SMS");
            }
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(request.title());
        announcement.setMessage(request.message());
        announcement.setCreatedByUserId(createdByUserId);
        announcement.setChannels(request.channels());
        announcement.setPhoto(request.photo());
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
                // Subject carries the announcement's title instead of the generic default, so it
                // actually reads as "Estate-wide water shutdown" in an inbox rather than the same
                // fixed subject every other notification uses.
                NotificationAttachment attachment = announcement.getPhoto() != null ? toAttachment(announcement.getPhoto()) : null;
                notificationService.dispatch(NotificationChannel.EMAIL, resident.getEmail(), announcement.getMessage(),
                        announcement.getTitle(), attachment);
            }
        }
    }

    /** Decodes a "data:image/jpeg;base64,..." URI into an EMAIL attachment — WhatsApp/SMS senders never see this (see NotificationAttachment). */
    private static NotificationAttachment toAttachment(String dataUri) {
        int comma = dataUri.indexOf(',');
        String header = dataUri.substring("data:".length(), comma);
        String contentType = header.substring(0, header.indexOf(';'));
        String extension = contentType.substring(contentType.indexOf('/') + 1);
        byte[] content = Base64.getDecoder().decode(dataUri.substring(comma + 1));
        return new NotificationAttachment("announcement." + extension, contentType, content);
    }
}
