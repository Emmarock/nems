package com.nitelestate.user;

import com.nitelestate.audit.AuditService;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.property.Property;
import com.nitelestate.property.PropertyRepository;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.user.dto.OfficeHolderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * The super admin assigns a resident to each estate office (every role except RESIDENT). Office
 * notifications use that resident's email when the office account has no personal email set
 * (OfficeNotificationService).
 */
@Service
@RequiredArgsConstructor
public class OfficeHolderService {

    private final OfficeHolderRepository officeHolderRepository;
    private final ResidentRepository residentRepository;
    private final PropertyRepository propertyRepository;
    private final AuditService auditService;

    /** Every office, assigned or not, in Role order. */
    @Transactional(readOnly = true)
    public List<OfficeHolderResponse> list() {
        return offices().stream()
                .map(role -> toResponse(role, officeHolderRepository.findByRole(role).orElse(null)))
                .toList();
    }

    @Transactional
    public OfficeHolderResponse assign(Role role, Long residentId, Long adminUserId) {
        requireOffice(role);
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> NotFoundException.of("Resident", residentId));
        OfficeHolder holder = officeHolderRepository.findByRole(role).orElseGet(OfficeHolder::new);
        holder.setRole(role);
        holder.setResidentId(resident.getId());
        holder.setAssignedByUserId(adminUserId);
        holder = officeHolderRepository.save(holder);
        auditService.record("OfficeHolder", holder.getId(), "ASSIGN", role + " -> resident " + resident.getId());
        return toResponse(role, holder);
    }

    @Transactional
    public OfficeHolderResponse unassign(Role role) {
        requireOffice(role);
        officeHolderRepository.findByRole(role).ifPresent(holder -> {
            officeHolderRepository.delete(holder);
            auditService.record("OfficeHolder", holder.getId(), "UNASSIGN", role.name());
        });
        return toResponse(role, null);
    }

    static List<Role> offices() {
        return Arrays.stream(Role.values()).filter(r -> r != Role.RESIDENT).toList();
    }

    private static void requireOffice(Role role) {
        if (role == Role.RESIDENT) {
            throw new BadRequestException("RESIDENT is not an estate office");
        }
    }

    private OfficeHolderResponse toResponse(Role role, OfficeHolder holder) {
        Resident resident = holder == null ? null : residentRepository.findById(holder.getResidentId()).orElse(null);
        if (resident == null) {
            return new OfficeHolderResponse(role, null, null, null, null, null);
        }
        String house = resident.getPropertyId() == null ? null
                : propertyRepository.findById(resident.getPropertyId()).map(Property::getHouseNumber).orElse(null);
        return new OfficeHolderResponse(role, resident.getId(), resident.getFullName(), resident.getEmail(),
                resident.getPhone(), house);
    }
}
