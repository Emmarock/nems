package com.nitelestate.household;

import com.nitelestate.access.AccessDirection;
import com.nitelestate.access.AccessEventService;
import com.nitelestate.access.AccessSubjectType;
import com.nitelestate.access.dto.AccessEventResponse;
import com.nitelestate.accesspolicy.AccessPolicyService;
import com.nitelestate.audit.AuditService;
import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.common.search.Specs;
import com.nitelestate.household.dto.HouseholdMemberLookupResponse;
import com.nitelestate.household.dto.HouseholdMemberRequest;
import com.nitelestate.household.dto.HouseholdMemberResponse;
import com.nitelestate.property.Property;
import com.nitelestate.property.PropertyRepository;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.user.Role;
import lombok.RequiredArgsConstructor;
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
public class HouseholdMemberService {

    private final HouseholdMemberRepository householdMemberRepository;
    private final ResidentRepository residentRepository;
    private final PropertyRepository propertyRepository;
    private final AccessEventService accessEventService;
    private final AccessPolicyService accessPolicyService;
    private final AuditService auditService;

    /** ~1.5MB of base64 — same cap as Worker/Visitor photos. */
    private static final int MAX_PHOTO_LENGTH = 2_000_000;

    /**
     * A resident sponsors a household member (spouse/family) for themselves — the property is
     * always the sponsor's own, resolved here rather than trusted from the request body, same as
     * WorkerService.request. Unlike Worker/Visitor, no QR is issued and no dates are set yet: the
     * property's landlord must approve() first.
     */
    @Transactional
    public HouseholdMemberResponse request(Long sponsorResidentId, HouseholdMemberRequest request) {
        if (request.photo() != null && request.photo().length() > MAX_PHOTO_LENGTH) {
            throw new BadRequestException("Photo is too large — please use a smaller image");
        }
        Resident sponsor = residentRepository.findById(sponsorResidentId)
                .orElseThrow(() -> NotFoundException.of("Resident", sponsorResidentId));
        if (sponsor.getPropertyId() == null) {
            throw new BadRequestException("You don't have a property on file yet — contact an admin to get one linked");
        }

        HouseholdMember member = new HouseholdMember();
        member.setFullName(request.fullName());
        member.setPhone(request.phone());
        member.setRelationship(request.relationship());
        member.setPropertyId(sponsor.getPropertyId());
        member.setSponsorResidentId(sponsorResidentId);
        member.setStatus(HouseholdMemberStatus.PENDING_APPROVAL);
        member.setPhoto(request.photo());
        member = householdMemberRepository.save(member);

        auditService.record("HouseholdMember", member.getId(), "REQUEST", member.getFullName());
        return HouseholdMemberResponse.from(member, resolveHouseNumber(member.getPropertyId()));
    }

    /**
     * The landlord (or an admin) approves the request, activating the pass and issuing its QR —
     * no validity window to set; the pass is valid indefinitely, same as the home owner's own
     * standing, until revoke() is called.
     */
    @Transactional
    public HouseholdMemberResponse approve(Long id, Long callerResidentId, Role callerRole) {
        HouseholdMember member = get(id);
        requireLandlordOrAdmin(member.getPropertyId(), callerResidentId, callerRole);

        member.setStatus(HouseholdMemberStatus.ACTIVE);
        member.setQrToken(UUID.randomUUID().toString());
        member = householdMemberRepository.save(member);

        auditService.record("HouseholdMember", member.getId(), "APPROVE", null);
        return HouseholdMemberResponse.from(member, resolveHouseNumber(member.getPropertyId()));
    }

    /** The sponsor, the property's landlord, or an admin pulls an already-approved pass, e.g. the household member moved out. */
    @Transactional
    public HouseholdMemberResponse revoke(Long id, Long callerResidentId, Role callerRole) {
        HouseholdMember member = get(id);
        requireLandlordAdminOrSponsor(member, callerResidentId, callerRole);

        member.setStatus(HouseholdMemberStatus.REVOKED);
        member = householdMemberRepository.save(member);

        auditService.record("HouseholdMember", member.getId(), "REVOKE", null);
        return HouseholdMemberResponse.from(member, resolveHouseNumber(member.getPropertyId()));
    }

    /**
     * Read-only gate-side scan: resolves the pass holder's destination (sponsor + property) so
     * security can confirm it before deciding whether to check the household member in. Does not
     * mutate status or write an access event — see checkIn for that.
     */
    public HouseholdMemberLookupResponse lookup(String qrToken) {
        HouseholdMember member = householdMemberRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No household member pass found for this QR code"));
        String flag = evaluate(member).flag();

        Resident sponsor = residentRepository.findById(member.getSponsorResidentId()).orElse(null);
        Property property = propertyRepository.findById(member.getPropertyId()).orElse(null);

        return new HouseholdMemberLookupResponse(member.getId(), member.getFullName(), member.getPhone(),
                member.getRelationship(), member.getStatus(), flag, member.getPhoto(),
                member.getSponsorResidentId(), sponsor != null ? sponsor.getFullName() : null,
                sponsor != null ? sponsor.getPhone() : null,
                property != null ? property.getId() : null, property != null ? property.getHouseNumber() : null,
                property != null ? property.getAddress() : null);
    }

    /**
     * Gate-side check-in; evaluates the approval status and flags (not blocks) anomalies - but
     * the sponsor's arrears status IS a hard block here (see
     * AccessPolicyService.requireSponsorEligibleForEntry); checking out is never denied by it.
     */
    @Transactional
    public HouseholdMemberResponse checkIn(String qrToken, Long gateId, Long verifiedByUserId) {
        HouseholdMember member = householdMemberRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No household member pass found for this QR code"));

        accessPolicyService.requireSponsorEligibleForEntry(member.getSponsorResidentId());
        String flag = evaluate(member).flag();
        accessEventService.record(AccessSubjectType.HOUSEHOLD_MEMBER, member.getId(), AccessDirection.IN, gateId,
                verifiedByUserId, flag);
        auditService.record("HouseholdMember", member.getId(), "CHECK_IN", flag);
        return HouseholdMemberResponse.from(member, resolveHouseNumber(member.getPropertyId()));
    }

    @Transactional
    public HouseholdMemberResponse checkOut(String qrToken, Long gateId, Long verifiedByUserId) {
        HouseholdMember member = householdMemberRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No household member pass found for this QR code"));

        accessEventService.record(AccessSubjectType.HOUSEHOLD_MEMBER, member.getId(), AccessDirection.OUT, gateId,
                verifiedByUserId, null);
        auditService.record("HouseholdMember", member.getId(), "CHECK_OUT", null);
        return HouseholdMemberResponse.from(member, resolveHouseNumber(member.getPropertyId()));
    }

    public PageResponse<AccessEventResponse> logs(Long memberId, Pageable pageable) {
        return accessEventService.search(AccessSubjectType.HOUSEHOLD_MEMBER, memberId, null, pageable);
    }

    public HouseholdMemberResponse findById(Long id) {
        HouseholdMember member = get(id);
        return HouseholdMemberResponse.from(member, resolveHouseNumber(member.getPropertyId()));
    }

    public PageResponse<HouseholdMemberResponse> search(String q, Long propertyId, Long sponsorResidentId, Pageable pageable) {
        Specification<HouseholdMember> spec = Specification.<HouseholdMember>where(Specs.contains(q, "fullName", "phone"))
                .and(Specs.eq(propertyId, "propertyId"))
                .and(Specs.eq(sponsorResidentId, "sponsorResidentId"));
        var page = householdMemberRepository.findAll(spec, pageable);
        return toResponsePage(page);
    }

    public PageResponse<HouseholdMemberResponse> mine(Long sponsorResidentId, Pageable pageable) {
        var page = householdMemberRepository.findAll(Specs.eq(sponsorResidentId, "sponsorResidentId"), pageable);
        return toResponsePage(page);
    }

    /**
     * Household members awaiting the caller's approval, scoped to the property they own.
     * v1 assumes one owned property per landlord (see PropertyRepository.findByOwnerId) — a
     * caller who owns nothing (e.g. a tenant who navigates here) simply sees an empty page.
     */
    public PageResponse<HouseholdMemberResponse> pendingForLandlord(Long callerResidentId, Pageable pageable) {
        List<Property> owned = propertyRepository.findByOwnerId(callerResidentId);
        if (owned.isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
        }
        Specification<HouseholdMember> spec = Specification.<HouseholdMember>where(Specs.eq(owned.get(0).getId(), "propertyId"))
                .and(Specs.eq(HouseholdMemberStatus.PENDING_APPROVAL, "status"));
        var page = householdMemberRepository.findAll(spec, pageable);
        return toResponsePage(page);
    }

    private PageResponse<HouseholdMemberResponse> toResponsePage(org.springframework.data.domain.Page<HouseholdMember> page) {
        List<Long> propertyIds = page.getContent().stream().map(HouseholdMember::getPropertyId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> houseNumbers = propertyRepository.findAllById(propertyIds).stream()
                .collect(Collectors.toMap(Property::getId, Property::getHouseNumber));
        return PageResponse.of(page.map(m -> HouseholdMemberResponse.from(m, houseNumbers.get(m.getPropertyId()))));
    }

    /** Only the landlord who owns this property, or an admin, may approve a pending request. */
    private void requireLandlordOrAdmin(Long propertyId, Long callerResidentId, Role callerRole) {
        if (callerRole == Role.SUPER_ADMIN || callerRole == Role.CDA_ADMIN) {
            return;
        }
        boolean owns = propertyRepository.findByOwnerId(callerResidentId).stream()
                .anyMatch(p -> p.getId().equals(propertyId));
        if (!owns) {
            throw new BadRequestException("Only the property's landlord (or an admin) can do this");
        }
    }

    /**
     * Revoking is open to a third party too, unlike approving: the sponsor who originally
     * requested the pass can pull it themselves (e.g. the family member moved out), without
     * needing to go through the landlord.
     */
    private void requireLandlordAdminOrSponsor(HouseholdMember member, Long callerResidentId, Role callerRole) {
        if (callerRole == Role.SUPER_ADMIN || callerRole == Role.CDA_ADMIN) {
            return;
        }
        if (callerResidentId != null && callerResidentId.equals(member.getSponsorResidentId())) {
            return;
        }
        boolean owns = propertyRepository.findByOwnerId(callerResidentId).stream()
                .anyMatch(p -> p.getId().equals(member.getPropertyId()));
        if (!owns) {
            throw new BadRequestException("Only the sponsor, the property's landlord, or an admin can do this");
        }
    }

    private String resolveHouseNumber(Long propertyId) {
        return propertyId == null ? null : propertyRepository.findById(propertyId).map(Property::getHouseNumber).orElse(null);
    }

    private HouseholdMember get(Long id) {
        return householdMemberRepository.findById(id).orElseThrow(() -> NotFoundException.of("HouseholdMember", id));
    }

    private record Evaluation(String flag) {
    }

    /** Pure evaluation of approval status — no date window to check; an ACTIVE pass is valid until revoked. */
    private Evaluation evaluate(HouseholdMember member) {
        if (member.getStatus() == HouseholdMemberStatus.REVOKED) {
            return new Evaluation("HOUSEHOLD_MEMBER_REVOKED");
        }
        if (member.getStatus() == HouseholdMemberStatus.PENDING_APPROVAL) {
            return new Evaluation("NOT_APPROVED");
        }
        return new Evaluation(null);
    }
}
