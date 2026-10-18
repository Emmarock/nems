package com.nitelestate.resident;

import com.nitelestate.access.AccessDirection;
import com.nitelestate.access.AccessEventService;
import com.nitelestate.access.AccessSubjectType;
import com.nitelestate.accesspolicy.AccessPolicyService;
import com.nitelestate.audit.AuditService;
import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.common.search.Specs;
import com.nitelestate.household.HouseholdMember;
import com.nitelestate.household.HouseholdMemberRepository;
import com.nitelestate.household.HouseholdMemberStatus;
import com.nitelestate.property.Property;
import com.nitelestate.property.PropertyRepository;
import com.nitelestate.resident.dto.MeProfileUpdateRequest;
import com.nitelestate.resident.dto.ResidentArrearsResponse;
import com.nitelestate.resident.dto.ResidentLookupResponse;
import com.nitelestate.resident.dto.ResidentRequest;
import com.nitelestate.resident.dto.ResidentResponse;
import com.nitelestate.resident.dto.TenantRequest;
import com.nitelestate.resident.dto.TenantResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.user.PhoneNumbers;
import com.nitelestate.user.UserRepository;
import com.nitelestate.user.UserService;
import com.nitelestate.user.UserStatus;
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
    private final UserRepository userRepository;
    private final UserService userService;
    /** Repository, not HouseholdMemberService — a plain Spring Data interface has no dependencies
     * of its own to form a cycle with, the same reasoning PaymentService uses for PropertyRepository. */
    private final HouseholdMemberRepository householdMemberRepository;

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
        boolean emailAdded = isBlank(resident.getEmail()) && !isBlank(request.email());
        apply(resident, request);
        resident = residentRepository.save(resident);
        syncLoginPhone(resident);
        syncLoginFullName(resident);
        // A resident captured without one (e.g. bulk-imported from a phone-only record) now has a
        // real address on file - let them know how to get in rather than leaving it a silent edit.
        if (emailAdded) {
            userService.notifyLoginDetails(resident);
        }
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
        boolean emailAdded = isBlank(resident.getEmail()) && !isBlank(request.email());
        resident.setFullName(request.fullName());
        resident.setPhone(request.phone());
        resident.setEmail(request.email());
        resident.setEmergencyContact(request.emergencyContact());
        resident = residentRepository.save(resident);
        syncLoginPhone(resident);
        syncLoginFullName(resident);
        // Most useful for a resident whose login still uses a synthetic placeholder email (see
        // UserService.buildLoginEmail) - this gives them a real "set your password" link the
        // moment they add a real address, not just a silently-updated profile field.
        if (emailAdded) {
            userService.notifyLoginDetails(resident);
        }
        auditService.record("Resident", resident.getId(), "SELF_UPDATE", resident.getFullName());
        return ResidentResponse.from(resident, resolvePropertyHouseNumber(resident.getPropertyId()));
    }

    /**
     * Keeps a resident's login phone (if their account already uses one, or is eligible to start
     * using one) in step with their profile - otherwise updating your contact number here could
     * silently break, or never grant, phone-based login. Skipped if the new number would collide
     * with another account's, same as bulk creation.
     */
    private void syncLoginPhone(Resident resident) {
        String normalized = PhoneNumbers.normalize(resident.getPhone());
        if (normalized == null) {
            return;
        }
        userRepository.findByResidentId(resident.getId()).ifPresent(user -> {
            if (!normalized.equals(user.getPhone()) && !userRepository.existsByPhone(normalized)) {
                user.setPhone(normalized);
                userRepository.save(user);
            }
        });
    }

    /**
     * Keeps a resident's login name in step with their profile - app_user.fullName is only ever
     * copied from Resident.fullName once, at account-creation/bulk-provisioning/claim time (see
     * UserService.create/bulkCreateResidentAccounts/verifyClaimAccount); nothing else pushes a
     * later rename back into the linked login row, so the header (read from the login's own
     * cached name) and the resident's actual profile silently fall out of step otherwise - same
     * class of bug syncLoginPhone already covers for the phone number.
     */
    private void syncLoginFullName(Resident resident) {
        userRepository.findByResidentId(resident.getId()).ifPresent(user -> {
            if (!resident.getFullName().equals(user.getFullName())) {
                user.setFullName(resident.getFullName());
                userRepository.save(user);
            }
        });
    }

    /**
     * A landlord adds a tenant to their own property, self-service — the property is resolved
     * from the properties the caller owns (see PropertyRepository.findByOwnerId), never trusted
     * from the request body. v1 assumes one owned property per landlord.
     *
     * Also provisions the tenant's login in the same step (reusing UserService's existing
     * bulk-create mechanism) so a landlord never needs an admin involved to get a tenant into the
     * portal — admins only need to onboard the landlord themselves.
     */
    @Transactional
    public TenantResponse createTenant(Long landlordResidentId, TenantRequest request) {
        Property property = requireOwnedProperty(landlordResidentId);
        if (property.getApartmentCount() != null) {
            long activeTenants = residentRepository.count(
                    Specification.<Resident>where(Specs.eq(property.getId(), "propertyId"))
                            .and(Specs.eq(ResidentType.TENANT, "residentType"))
                            .and(Specs.eq(ResidentStatus.ACTIVE, "status")));
            if (activeTenants >= property.getApartmentCount()) {
                throw new BadRequestException("You've reached your configured apartment limit of "
                        + property.getApartmentCount() + " — deactivate a tenant or increase your apartment count first");
            }
        }

        Resident tenant = new Resident();
        tenant.setFullName(request.fullName());
        tenant.setPhone(request.phone());
        tenant.setEmail(request.email());
        tenant.setEmergencyContact(request.emergencyContact());
        tenant.setPropertyId(property.getId());
        tenant.setResidentType(ResidentType.TENANT);
        tenant = residentRepository.save(tenant);

        auditService.record("Resident", tenant.getId(), "LANDLORD_ADD_TENANT", tenant.getFullName());

        var accounts = userService.bulkCreateResidentAccounts(List.of(tenant.getId())).created();
        var account = accounts.isEmpty() ? null : accounts.get(0);

        return new TenantResponse(ResidentResponse.from(tenant, property.getHouseNumber()),
                account != null ? account.email() : null,
                account != null ? account.phone() : null,
                account != null ? account.temporaryPassword() : null);
    }

    /** Tenants currently on the caller's own property. */
    public PageResponse<ResidentResponse> listTenants(Long landlordResidentId, Pageable pageable) {
        Property property = requireOwnedProperty(landlordResidentId);
        Specification<Resident> spec = Specification.<Resident>where(Specs.eq(property.getId(), "propertyId"))
                .and(Specs.eq(ResidentType.TENANT, "residentType"));
        var page = residentRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(r -> ResidentResponse.from(r, property.getHouseNumber())));
    }

    /**
     * Soft-removes a tenant from the caller's own property: marks them INACTIVE and disables
     * their login (if one exists) — no hard delete, so billing/access history is preserved.
     */
    @Transactional
    public ResidentResponse deactivateTenant(Long landlordResidentId, Long tenantId) {
        Property property = requireOwnedProperty(landlordResidentId);
        Resident tenant = get(tenantId);
        if (tenant.getResidentType() != ResidentType.TENANT || !property.getId().equals(tenant.getPropertyId())) {
            throw new BadRequestException("That resident isn't a tenant on a property you own");
        }

        tenant.setStatus(ResidentStatus.INACTIVE);
        tenant = residentRepository.save(tenant);
        userRepository.findByResidentId(tenant.getId())
                .ifPresent(user -> userService.setStatus(user.getId(), UserStatus.DISABLED));
        revokeSponsoredHouseholdMembers(tenant.getId());

        auditService.record("Resident", tenant.getId(), "LANDLORD_DEACTIVATE_TENANT", tenant.getFullName());
        return ResidentResponse.from(tenant, property.getHouseNumber());
    }

    /**
     * Admin/security action - any resident (owner or tenant), not just a landlord deactivating
     * their own tenant. Same cascade as deactivateTenant: login disabled, sponsored household
     * passes revoked, so a suspension actually keeps everyone it covers out rather than just the
     * one resident whose own pass gets flagged.
     */
    @Transactional
    public ResidentResponse suspend(Long residentId) {
        Resident resident = get(residentId);
        resident.setStatus(ResidentStatus.SUSPENDED);
        resident = residentRepository.save(resident);
        userRepository.findByResidentId(resident.getId())
                .ifPresent(user -> userService.setStatus(user.getId(), UserStatus.DISABLED));
        revokeSponsoredHouseholdMembers(resident.getId());

        auditService.record("Resident", resident.getId(), "SUSPEND", resident.getFullName());
        return ResidentResponse.from(resident, resolvePropertyHouseNumber(resident.getPropertyId()));
    }

    /**
     * Lifts a suspension - login re-enabled, but sponsored household passes stay REVOKED (the
     * landlord re-approves whichever ones should come back, same as any other revoked pass;
     * nothing here silently re-grants gate access on a household member's behalf).
     */
    @Transactional
    public ResidentResponse reactivate(Long residentId) {
        Resident resident = get(residentId);
        resident.setStatus(ResidentStatus.ACTIVE);
        resident = residentRepository.save(resident);
        userRepository.findByResidentId(resident.getId())
                .ifPresent(user -> userService.setStatus(user.getId(), UserStatus.ACTIVE));

        auditService.record("Resident", resident.getId(), "REACTIVATE", resident.getFullName());
        return ResidentResponse.from(resident, resolvePropertyHouseNumber(resident.getPropertyId()));
    }

    /**
     * A deactivated tenant's own household passes (spouse/family QR codes they sponsored) go
     * with them — otherwise a family member's pass stays silently ACTIVE after the tenant who
     * vouched for them is gone, with nothing at the gate ever questioning it. Already-revoked
     * entries are left alone rather than re-audited.
     */
    private void revokeSponsoredHouseholdMembers(Long tenantResidentId) {
        List<HouseholdMember> sponsored = householdMemberRepository.findAll(
                Specification.<HouseholdMember>where(Specs.eq(tenantResidentId, "sponsorResidentId"))
                        .and(Specs.in(List.of(HouseholdMemberStatus.ACTIVE, HouseholdMemberStatus.PENDING_APPROVAL), "status")));
        for (HouseholdMember member : sponsored) {
            member.setStatus(HouseholdMemberStatus.REVOKED);
            householdMemberRepository.save(member);
            auditService.record("HouseholdMember", member.getId(), "REVOKE_ON_TENANT_DEACTIVATION", member.getFullName());
        }
    }

    /**
     * The forward direction of requireOwnedProperty: given a resident who OCCUPIES a property,
     * resolve the resident id of whoever OWNS it — a tenant's landlord. Called by the landlord
     * themselves it just returns their own id (property.ownerId == their own residentId),
     * which is harmless — see MeController's "my landlord's account" endpoints.
     */
    public Long resolveLandlordResidentId(Long residentId) {
        Resident resident = get(residentId);
        if (resident.getPropertyId() == null) {
            throw new BadRequestException("You don't have a property on file yet — contact an admin to get one linked");
        }
        Property property = propertyRepository.findById(resident.getPropertyId())
                .orElseThrow(() -> NotFoundException.of("Property", resident.getPropertyId()));
        if (property.getOwnerId() == null) {
            throw new BadRequestException("Your property doesn't have a landlord on file yet — contact an admin");
        }
        return property.getOwnerId();
    }

    /** The single property the caller owns — v1 doesn't support a landlord owning more than one. */
    private Property requireOwnedProperty(Long landlordResidentId) {
        List<Property> owned = propertyRepository.findByOwnerId(landlordResidentId);
        if (owned.isEmpty()) {
            throw new BadRequestException("You don't own a property on file — contact an admin to get one linked");
        }
        return owned.get(0);
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
     * and balance formula as {@link com.nitelestate.estatesecurity.SecurityDashboardService},
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

    /**
     * Every resident in arrears, unpaginated — backs the downloadable PDF report
     * (generateArrearsReportPdf) and the weekly reminder email (scheduling.ArrearsReminderJob),
     * both of which need the whole list at once rather than one browsable page. Same threshold/
     * enforcement check as searchInArrears, so all three stay in agreement about who's in arrears.
     */
    public List<ResidentArrearsResponse> allInArrears() {
        var settings = accessPolicyService.getSettings();
        if (!settings.enforceArrears()) {
            return List.of();
        }
        List<ResidentRepository.ResidentArrearsRow> rows = residentRepository.findAllResidentsInArrears(settings.arrearsThreshold());

        List<Long> residentIds = rows.stream().map(ResidentRepository.ResidentArrearsRow::getId).toList();
        Map<Long, Resident> residents = residentRepository.findAllById(residentIds).stream()
                .collect(Collectors.toMap(Resident::getId, r -> r));

        List<Long> propertyIds = residents.values().stream().map(Resident::getPropertyId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> houseNumbers = propertyRepository.findAllById(propertyIds).stream()
                .collect(Collectors.toMap(Property::getId, Property::getHouseNumber));

        return rows.stream().map(row -> {
            Resident r = residents.get(row.getId());
            return new ResidentArrearsResponse(r.getId(), r.getFullName(), r.getPhone(), r.getPropertyId(),
                    houseNumbers.get(r.getPropertyId()), row.getOutstanding());
        }).toList();
    }

    /** Downloadable PDF snapshot of every account currently in arrears — see ResidentArrearsReportPdf. */
    public byte[] generateArrearsReportPdf() {
        return ResidentArrearsReportPdf.render(allInArrears(), accessPolicyService.getSettings().arrearsThreshold());
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
        String flag = evaluateResidentFlag(resident);

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

        String flag = evaluateResidentFlag(resident);
        accessEventService.record(AccessSubjectType.RESIDENT, resident.getId(), AccessDirection.IN, gateId,
                verifiedByUserId, flag);
        auditService.record("Resident", resident.getId(), "CHECK_IN", flag);
        return ResidentResponse.from(resident, resolvePropertyHouseNumber(resident.getPropertyId()));
    }

    /**
     * INACTIVE (e.g. a deactivated tenant) takes priority over the arrears check and skips it
     * entirely - same early-return shape as VehicleService.evaluateVehicle's INACTIVE check.
     * Before this, a deactivated resident's own pass carried no flag at all at the gate; only
     * their login was disabled, which security scanning a physical/screenshotted QR never sees.
     */
    private String evaluateResidentFlag(Resident resident) {
        if (resident.getStatus() == ResidentStatus.SUSPENDED) {
            return "RESIDENT_SUSPENDED";
        }
        if (resident.getStatus() == ResidentStatus.INACTIVE) {
            return "RESIDENT_INACTIVE";
        }
        return accessPolicyService.evaluateResident(resident.getId());
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
