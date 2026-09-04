package com.cyrev.nitelestate.visitor;

import com.cyrev.nitelestate.access.AccessDirection;
import com.cyrev.nitelestate.access.AccessEventService;
import com.cyrev.nitelestate.access.AccessSubjectType;
import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.property.Property;
import com.cyrev.nitelestate.property.PropertyRepository;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.visitor.dto.VisitorLookupResponse;
import com.cyrev.nitelestate.visitor.dto.VisitorRequest;
import com.cyrev.nitelestate.visitor.dto.VisitorResponse;
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
    private final AuditService auditService;

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
        visitor.setQrToken(UUID.randomUUID().toString());
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
     * security can confirm it before deciding whether to check the visitor in. No side effects.
     */
    public VisitorLookupResponse lookup(String qrToken) {
        Visitor visitor = visitorRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No visitor pass found for this QR code"));
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
                property != null ? property.getAddress() : null);
    }

    /** Gate-side check-in; flags expired/out-of-window/cancelled passes rather than blocking outright. */
    @Transactional
    public VisitorResponse checkIn(String qrToken, Long gateId, Long verifiedByUserId) {
        Visitor visitor = visitorRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No visitor pass found for this QR code"));

        String flag = evaluateWindow(visitor);
        accessEventService.record(AccessSubjectType.VISITOR, visitor.getId(), AccessDirection.IN, gateId,
                verifiedByUserId, flag);
        auditService.record("Visitor", visitor.getId(), "CHECK_IN", flag);
        return VisitorResponse.from(visitor, resolveHostName(visitor.getHostResidentId()));
    }

    @Transactional
    public VisitorResponse checkOut(String qrToken, Long gateId, Long verifiedByUserId) {
        Visitor visitor = visitorRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No visitor pass found for this QR code"));

        accessEventService.record(AccessSubjectType.VISITOR, visitor.getId(), AccessDirection.OUT, gateId,
                verifiedByUserId, null);
        auditService.record("Visitor", visitor.getId(), "CHECK_OUT", null);
        return VisitorResponse.from(visitor, resolveHostName(visitor.getHostResidentId()));
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
