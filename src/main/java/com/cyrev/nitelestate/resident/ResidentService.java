package com.cyrev.nitelestate.resident;

import com.cyrev.nitelestate.access.AccessDirection;
import com.cyrev.nitelestate.access.AccessEventService;
import com.cyrev.nitelestate.access.AccessSubjectType;
import com.cyrev.nitelestate.accesspolicy.AccessPolicyService;
import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.property.Property;
import com.cyrev.nitelestate.property.PropertyRepository;
import com.cyrev.nitelestate.resident.dto.MeProfileUpdateRequest;
import com.cyrev.nitelestate.resident.dto.ResidentArrearsResponse;
import com.cyrev.nitelestate.resident.dto.ResidentLookupResponse;
import com.cyrev.nitelestate.resident.dto.ResidentRequest;
import com.cyrev.nitelestate.resident.dto.ResidentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository residentRepository;
    private final PropertyRepository propertyRepository;
    private final AccessEventService accessEventService;
    private final AccessPolicyService accessPolicyService;
    private final AuditService auditService;

    @Transactional
    public ResidentResponse create(ResidentRequest request) {
        Resident resident = new Resident();
        apply(resident, request);
        resident = residentRepository.save(resident);
        auditService.record("Resident", resident.getId(), "CREATE", resident.getFullName());
        return ResidentResponse.from(resident, resolvePropertyHouseNumber(resident.getPropertyId()));
    }

    @Transactional
    public ResidentResponse update(Long id, ResidentRequest request) {
        Resident resident = get(id);
        apply(resident, request);
        resident = residentRepository.save(resident);
        auditService.record("Resident", resident.getId(), "UPDATE", resident.getFullName());
        return ResidentResponse.from(resident, resolvePropertyHouseNumber(resident.getPropertyId()));
    }

    public ResidentResponse findById(Long id) {
        Resident resident = get(id);
        return ResidentResponse.from(resident, resolvePropertyHouseNumber(resident.getPropertyId()));
    }

    /** Self-service profile edit — the id is the caller's own (see MeController), never client-chosen. */
    @Transactional
    public ResidentResponse updateSelf(Long residentId, MeProfileUpdateRequest request) {
        Resident resident = get(residentId);
        resident.setFullName(request.fullName());
        resident.setPhone(request.phone());
        resident.setEmail(request.email());
        resident.setEmergencyContact(request.emergencyContact());
        resident = residentRepository.save(resident);
        auditService.record("Resident", resident.getId(), "SELF_UPDATE", resident.getFullName());
        return ResidentResponse.from(resident, resolvePropertyHouseNumber(resident.getPropertyId()));
    }

    public PageResponse<ResidentResponse> search(String q, Long propertyId, Pageable pageable) {
        Specification<Resident> spec = Specification.<Resident>where(Specs.contains(q, "fullName", "phone", "email"))
                .and(Specs.eq(propertyId, "propertyId"));
        var page = residentRepository.findAll(spec, pageable);

        List<Long> propertyIds = page.getContent().stream().map(Resident::getPropertyId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> houseNumbers = propertyRepository.findAllById(propertyIds).stream()
                .collect(Collectors.toMap(Property::getId, Property::getHouseNumber));

        return PageResponse.of(page.map(r -> ResidentResponse.from(r, houseNumbers.get(r.getPropertyId()))));
    }

    /**
     * "Accounts in arrears" drill-through for the security dashboard stat card — same threshold
     * and balance formula as {@link com.cyrev.nitelestate.estatesecurity.SecurityDashboardService},
     * so the list always matches the count shown there. If arrears enforcement is switched off in
     * Access Policy settings, there's nothing to show (mirrors the dashboard's own accountsInArrears=0).
     */
    public PageResponse<ResidentArrearsResponse> searchInArrears(String q, Pageable pageable) {
        var settings = accessPolicyService.getSettings();
        if (!settings.enforceArrears()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
        }

        Page<ResidentRepository.ResidentArrearsRow> page =
                residentRepository.findResidentsInArrears(settings.arrearsThreshold(), q == null ? "" : q.trim(), pageable);

        List<Long> residentIds = page.getContent().stream().map(ResidentRepository.ResidentArrearsRow::getId).toList();
        Map<Long, Resident> residents = residentRepository.findAllById(residentIds).stream()
                .collect(Collectors.toMap(Resident::getId, r -> r));

        List<Long> propertyIds = residents.values().stream().map(Resident::getPropertyId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> houseNumbers = propertyRepository.findAllById(propertyIds).stream()
                .collect(Collectors.toMap(Property::getId, Property::getHouseNumber));

        return PageResponse.of(page.map(row -> {
            Resident r = residents.get(row.getId());
            return new ResidentArrearsResponse(r.getId(), r.getFullName(), r.getPhone(), r.getPropertyId(),
                    houseNumbers.get(r.getPropertyId()), row.getOutstanding());
        }));
    }

    private String resolvePropertyHouseNumber(Long propertyId) {
        return propertyId == null ? null : propertyRepository.findById(propertyId).map(Property::getHouseNumber).orElse(null);
    }

    /** A resident's own QR access pass, generated the first time it's asked for. */
    @Transactional
    public String getOrCreateQrToken(Long residentId) {
        Resident resident = get(residentId);
        if (resident.getQrToken() == null) {
            resident.setQrToken(UUID.randomUUID().toString());
            resident = residentRepository.save(resident);
            auditService.record("Resident", resident.getId(), "ISSUE_ACCESS_PASS", null);
        }
        return resident.getQrToken();
    }

    /**
     * Read-only gate-side scan: the resident IS the destination here, so this just resolves
     * their own property plus the same arrears flag the generic RESIDENT access-event flow uses.
     */
    public ResidentLookupResponse lookup(String qrToken) {
        Resident resident = residentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No resident pass found for this QR code"));
        String flag = accessPolicyService.evaluateResident(resident.getId());

        Property property = resident.getPropertyId() != null
                ? propertyRepository.findById(resident.getPropertyId()).orElse(null) : null;

        return new ResidentLookupResponse(resident.getId(), resident.getFullName(), resident.getPhone(),
                resident.getResidentType(), resident.getStatus(), flag,
                property != null ? property.getId() : null, property != null ? property.getHouseNumber() : null,
                property != null ? property.getAddress() : null);
    }

    @Transactional
    public ResidentResponse checkIn(String qrToken, Long gateId, Long verifiedByUserId) {
        Resident resident = residentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No resident pass found for this QR code"));

        String flag = accessPolicyService.evaluateResident(resident.getId());
        accessEventService.record(AccessSubjectType.RESIDENT, resident.getId(), AccessDirection.IN, gateId,
                verifiedByUserId, flag);
        auditService.record("Resident", resident.getId(), "CHECK_IN", flag);
        return ResidentResponse.from(resident, resolvePropertyHouseNumber(resident.getPropertyId()));
    }

    @Transactional
    public ResidentResponse checkOut(String qrToken, Long gateId, Long verifiedByUserId) {
        Resident resident = residentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No resident pass found for this QR code"));

        accessEventService.record(AccessSubjectType.RESIDENT, resident.getId(), AccessDirection.OUT, gateId,
                verifiedByUserId, null);
        auditService.record("Resident", resident.getId(), "CHECK_OUT", null);
        return ResidentResponse.from(resident, resolvePropertyHouseNumber(resident.getPropertyId()));
    }

    Resident get(Long id) {
        return residentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Resident", id));
    }

    private void apply(Resident resident, ResidentRequest request) {
        resident.setFullName(request.fullName());
        resident.setPhone(request.phone());
        resident.setEmail(request.email());
        resident.setPropertyId(request.propertyId());
        resident.setResidentType(request.residentType());
        resident.setEmergencyContact(request.emergencyContact());
    }
}
