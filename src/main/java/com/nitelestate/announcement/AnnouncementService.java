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
import com.nitelestate.resident.ResidentType;
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

    /** ~4.5MB of base64 — generous enough for a multi-page meeting-minutes PDF, not just a photo. */
    private static final int MAX_ATTACHMENT_LENGTH = 6_000_000;

    @Transactional
    public AnnouncementResponse create(Long createdByUserId, AnnouncementRequest request) {
        if (request.attachment() != null) {
            if (request.attachment().length() > MAX_ATTACHMENT_LENGTH) {
                throw new BadRequestException("Attachment is too large — please use a smaller file");
            }
            String contentType = contentTypeOf(request.attachment());
            if (!contentType.startsWith("image/") && !contentType.equals("application/pdf")) {
                throw new BadRequestException("Only images or PDF files can be attached to an announcement");
            }
            if (request.channels().contains(NotificationChannel.SMS)) {
                throw new BadRequestException("An announcement with an attachment can't be sent by SMS — remove it or deselect SMS");
            }
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(request.title());
        announcement.setMessage(request.message());
        announcement.setCreatedByUserId(createdByUserId);
        announcement.setChannels(request.channels());
        announcement.setAttachment(request.attachment());
        announcement.setAttachmentFilename(request.attachmentFilename());
        // Unspecified defaults to false - an announcement has to opt tenants in, not opt them out.
        announcement.setIncludeTenants(Boolean.TRUE.equals(request.includeTenants()));
        announcement = announcementRepository.save(announcement);

        broadcast(announcement);

        auditService.record("Announcement", announcement.getId(), "CREATE", announcement.getTitle());
        return AnnouncementResponse.from(announcement, false);
    }

    /**
     * callerResidentId is null for staff (admins etc. - always see every announcement
     * regardless of audience) and set for a RESIDENT caller, whose own residentType then decides
     * whether a tenants-excluded announcement is even in their list at all - not just missing the
     * SMS/email, invisible outright (see Announcement.includeTenants).
     */
    public PageResponse<AnnouncementResponse> search(Long currentUserId, Long callerResidentId, Pageable pageable) {
        Set<Long> readIds = new HashSet<>(announcementReadRepository.findAnnouncementIdsByUserId(currentUserId));
        var page = isTenant(callerResidentId)
                ? announcementRepository.findByIncludeTenantsTrue(pageable)
                : announcementRepository.findAll(pageable);
        return PageResponse.of(page.map(a -> AnnouncementResponse.from(a, readIds.contains(a.getId()))));
    }

    public long unreadCount(Long currentUserId, Long callerResidentId) {
        long total = isTenant(callerResidentId) ? announcementRepository.countByIncludeTenantsTrue() : announcementRepository.count();
        long read = announcementReadRepository.countByUserId(currentUserId);
        return Math.max(0, total - read);
    }

    private boolean isTenant(Long residentId) {
        return residentId != null && residentRepository.findById(residentId)
                .map(r -> r.getResidentType() == ResidentType.TENANT)
                .orElse(false);
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
                .filter(r -> r.getStatus() == ResidentStatus.ACTIVE)
                .filter(r -> announcement.isIncludeTenants() || r.getResidentType() != ResidentType.TENANT)
                .toList();

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
                NotificationAttachment attachment = announcement.getAttachment() != null
                        ? toAttachment(announcement.getAttachment(), announcement.getAttachmentFilename())
                        : null;
                notificationService.dispatch(NotificationChannel.EMAIL, resident.getEmail(), announcement.getMessage(),
                        announcement.getTitle(), attachment);
            }
        }
    }

    /** The "image/jpeg" part of a "data:image/jpeg;base64,..." URI. */
    private static String contentTypeOf(String dataUri) {
        String header = dataUri.substring("data:".length(), dataUri.indexOf(','));
        return header.substring(0, header.indexOf(';'));
    }

    /** Decodes a "data:image/jpeg;base64,..." (or application/pdf) URI into an EMAIL attachment —
     * WhatsApp/SMS senders never see this (see NotificationAttachment). */
    private static NotificationAttachment toAttachment(String dataUri, String filename) {
        String contentType = contentTypeOf(dataUri);
        if (filename == null || filename.isBlank()) {
            String extension = contentType.equals("application/pdf") ? "pdf" : contentType.substring(contentType.indexOf('/') + 1);
            filename = "announcement." + extension;
        }
        byte[] content = Base64.getDecoder().decode(dataUri.substring(dataUri.indexOf(',') + 1));
        return new NotificationAttachment(filename, contentType, content);
    }
}
