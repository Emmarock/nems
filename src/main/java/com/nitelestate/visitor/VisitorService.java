package com.nitelestate.visitor;

import com.nitelestate.access.AccessDirection;
import com.nitelestate.access.AccessEventService;
import com.nitelestate.access.AccessSubjectType;
import com.nitelestate.accesspolicy.AccessPolicyService;
import com.nitelestate.audit.AuditService;
import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.common.search.Specs;
import com.nitelestate.notification.MessageTemplates;
import com.nitelestate.notification.NotificationChannel;
import com.nitelestate.notification.NotificationService;
import com.nitelestate.notification.UserNotificationService;
import com.nitelestate.property.Property;
import com.nitelestate.property.PropertyRepository;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.user.UserRepository;
import com.nitelestate.visitor.dto.VisitorLookupResponse;
import com.nitelestate.visitor.dto.VisitorRequest;
import com.nitelestate.visitor.dto.VisitorResponse;
import com.nitelestate.visitor.dto.VisitorScanResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitorService {

    /** ~1.5MB of base64 — comfortably covers a compressed photo (the frontend downscales before upload). */
    private static final int MAX_PHOTO_LENGTH = 2_000_000;

    private final VisitorRepository visitorRepository;
    private final ResidentRepository residentRepository;
    private final PropertyRepository propertyRepository;
    private final AccessEventService accessEventService;
    private final AccessPolicyService accessPolicyService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;
    private final UserRepository userRepository;

    @Transactional
    public VisitorResponse create(Long hostResidentId, VisitorRequest request) {
        if (!request.validUntil().isAfter(request.validFrom())) {
            throw new BadRequestException("validUntil must be after validFrom");
        }
        if (request.photo() != null && request.photo().length() > MAX_PHOTO_LENGTH) {
            throw new BadRequestException("Photo is too large — please use a smaller image");
        }
        Visitor visitor = new Visitor();
        visitor.setName(request.name());
        visitor.setPhone(request.phone());
        visitor.setVehiclePlate(request.vehiclePlate());
        visitor.setHostResidentId(hostResidentId);
        visitor.setValidFrom(request.validFrom());
        visitor.setValidUntil(request.validUntil());
        visitor.setQrTokenEntry(UUID.randomUUID().toString());
        // qrTokenExit is deliberately left null here — see scan(): it's generated on first
        // check-in, so a resident can only ever share it once there's something to exit from,
        // and a visitor who never shows up never gets one generated at all.
        visitor.setStatus(VisitorStatus.ACTIVE);
        visitor.setPhoto(request.photo());
        visitor = visitorRepository.save(visitor);

        auditService.record("Visitor", visitor.getId(), "CREATE_PASS", visitor.getName());
        return VisitorResponse.from(visitor, resolveHostName(visitor.getHostResidentId()));
    }

    @Transactional
    public VisitorResponse cancel(Long id, Long hostResidentId) {
        Visitor visitor = get(id);
        requireHost(visitor, hostResidentId);
        visitor.setStatus(VisitorStatus.CANCELLED);
        visitor = visitorRepository.save(visitor);
        auditService.record("Visitor", visitor.getId(), "CANCEL", null);
        return VisitorResponse.from(visitor, resolveHostName(visitor.getHostResidentId()));
    }

    public VisitorResponse findById(Long id) {
        Visitor visitor = get(id);
        return VisitorResponse.from(visitor, resolveHostName(visitor.getHostResidentId()));
    }

    /** {@code hostResidentId} narrows to one resident's own passes ("My Visitors"); {@code q}
     * is the admin/security list's free-text search over name/phone - the two are independent. */
    public PageResponse<VisitorResponse> search(Long hostResidentId, String q, Pageable pageable) {
        Specification<Visitor> spec = Specification.<Visitor>where(Specs.eq(hostResidentId, "hostResidentId"))
                .and(Specs.contains(q, "name", "phone"));
        var page = visitorRepository.findAll(spec, pageable);

        List<Long> hostIds = page.getContent().stream().map(Visitor::getHostResidentId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> hostNames = residentRepository.findAllById(hostIds).stream()
                .collect(Collectors.toMap(Resident::getId, Resident::getFullName));

        return PageResponse.of(page.map(v -> VisitorResponse.from(v, hostNames.get(v.getHostResidentId()))));
    }

    private String resolveHostName(Long hostResidentId) {
        return hostResidentId == null ? null
                : residentRepository.findById(hostResidentId).map(Resident::getFullName).orElse(null);
    }

    /**
     * Read-only gate-side scan: resolves the pass holder's destination (host + property) so
     * security can confirm it, plus the one direction this particular QR code is for.
     * No side effects.
     */
    public VisitorLookupResponse lookup(String qrToken) {
        ResolvedVisitor resolved = resolve(qrToken);
        Visitor visitor = resolved.visitor();
        String flag = evaluateWindow(visitor);

        Resident host = residentRepository.findById(visitor.getHostResidentId()).orElse(null);
        Property property = host != null && host.getPropertyId() != null
                ? propertyRepository.findById(host.getPropertyId()).orElse(null) : null;

        return new VisitorLookupResponse(visitor.getId(), visitor.getName(), visitor.getPhone(),
                visitor.getVehiclePlate(), visitor.getValidFrom(), visitor.getValidUntil(), visitor.getStatus(), flag,
                visitor.getPhoto(),
                visitor.getHostResidentId(), host != null ? host.getFullName() : null,
                host != null ? host.getPhone() : null,
                property != null ? property.getId() : null, property != null ? property.getHouseNumber() : null,
                property != null ? property.getAddress() : null,
                resolved.direction());
    }

    /**
     * Gate-side scan. Each visitor pass carries two QR codes — one that only ever checks the
     * visitor in, one that only ever checks them out — so which token was scanned fixes the
     * direction; the caller can't ask for the other one. Flags expired/out-of-window/cancelled
     * passes on check-in rather than blocking outright, same as before. The host's arrears
     * status, however, IS a hard block on check-in (see requireSponsorEligibleForEntry) - once
     * the visitor is already inside, checking out is never denied by it.
     */
    @Transactional
    public VisitorScanResponse scan(String qrToken, Long gateId, Long verifiedByUserId) {
        ResolvedVisitor resolved = resolve(qrToken);
        Visitor visitor = resolved.visitor();
        AccessDirection direction = resolved.direction();

        if (direction == AccessDirection.IN) {
            accessPolicyService.requireSponsorEligibleForEntry(visitor.getHostResidentId());
        }
        String flag = direction == AccessDirection.IN ? evaluateWindow(visitor) : null;
        accessEventService.record(AccessSubjectType.VISITOR, visitor.getId(), direction, gateId, verifiedByUserId, flag);

        // First real check-in is what unlocks the exit pass — generate it here, once, rather
        // than at creation time (see the comment on Visitor.qrTokenExit).
        if (direction == AccessDirection.IN && visitor.getQrTokenExit() == null) {
            visitor.setQrTokenExit(UUID.randomUUID().toString());
            visitor = visitorRepository.save(visitor);
        }

        auditService.record("Visitor", visitor.getId(), direction == AccessDirection.IN ? "CHECK_IN" : "CHECK_OUT", flag);
        notifyHostOfScan(visitor, direction);
        return new VisitorScanResponse(VisitorResponse.from(visitor, resolveHostName(visitor.getHostResidentId())), direction);
    }

    /**
     * Lets the host resident know the moment their visitor actually arrives or leaves — in-app
     * and SMS, same two channels either direction, regardless of whether the host is the
     * property owner or a sponsoring tenant.
     */
    private void notifyHostOfScan(Visitor visitor, AccessDirection direction) {
        Resident host = residentRepository.findById(visitor.getHostResidentId()).orElse(null);
        if (host == null) {
            return;
        }
        boolean in = direction == AccessDirection.IN;
        String smsMessage = in
                ? MessageTemplates.visitorCheckedIn(host.getFullName(), visitor.getName())
                : MessageTemplates.visitorCheckedOut(host.getFullName(), visitor.getName());
        notificationService.dispatch(NotificationChannel.SMS, host.getPhone(), smsMessage);

        String title = in ? "Visitor checked in" : "Visitor checked out";
        String message = visitor.getName() + " has just checked " + (in ? "in" : "out") + " at the gate.";
        userRepository.findByResidentId(host.getId()).ifPresent(u -> userNotificationService.notify(u.getId(), title, message));
    }

    /** Tries the entry code first, then the exit code — the one that matches fixes the direction. */
    private ResolvedVisitor resolve(String qrToken) {
        return visitorRepository.findByQrTokenEntry(qrToken).map(v -> new ResolvedVisitor(v, AccessDirection.IN))
                .or(() -> visitorRepository.findByQrTokenExit(qrToken).map(v -> new ResolvedVisitor(v, AccessDirection.OUT)))
                .orElseThrow(() -> new NotFoundException("No visitor pass found for this QR code"));
    }

    private record ResolvedVisitor(Visitor visitor, AccessDirection direction) {
    }

    private String evaluateWindow(Visitor visitor) {
        Instant now = Instant.now();
        if (visitor.getStatus() == VisitorStatus.CANCELLED) {
            return "PASS_CANCELLED";
        }
        if (now.isBefore(visitor.getValidFrom()) || now.isAfter(visitor.getValidUntil())) {
            return "OUTSIDE_VALID_WINDOW";
        }
        return null;
    }

    private void requireHost(Visitor visitor, Long hostResidentId) {
        if (!visitor.getHostResidentId().equals(hostResidentId)) {
            throw new BadRequestException("You can only manage visitor passes you created");
        }
    }

    private Visitor get(Long id) {
        return visitorRepository.findById(id).orElseThrow(() -> NotFoundException.of("Visitor", id));
    }
}
