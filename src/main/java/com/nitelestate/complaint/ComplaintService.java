package com.nitelestate.complaint;

import com.nitelestate.audit.AuditService;
import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.common.search.Specs;
import com.nitelestate.complaint.dto.ComplaintRequest;
import com.nitelestate.complaint.dto.ComplaintResponse;
import com.nitelestate.complaint.dto.ComplaintUpdateRequest;
import com.nitelestate.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final AuditService auditService;

    private static final List<ComplaintStatus> OPEN_STATUSES =
            List.of(ComplaintStatus.OPEN, ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS);

    @Transactional
    public ComplaintResponse create(Long residentId, ComplaintRequest request) {
        Complaint complaint = new Complaint();
        complaint.setResidentId(residentId);
        complaint.setCategory(request.category());
        complaint.setDescription(request.description());
        complaint.setPriority(request.priority() != null ? request.priority() : ComplaintPriority.MEDIUM);
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint = complaintRepository.save(complaint);

        auditService.record("Complaint", complaint.getId(), "CREATE", complaint.getCategory().name());
        return ComplaintResponse.from(complaint);
    }

    @Transactional
    public ComplaintResponse updateStatus(Long id, ComplaintUpdateRequest request) {
        Complaint complaint = get(id);
        complaint.setStatus(request.status());
        if (request.status() == ComplaintStatus.RESOLVED || request.status() == ComplaintStatus.CLOSED) {
            complaint.setResolvedAt(Instant.now());
        }
        complaint = complaintRepository.save(complaint);

        auditService.record("Complaint", complaint.getId(), "STATUS_" + request.status(), null);
        return ComplaintResponse.from(complaint);
    }

    /** Routes a complaint to a staff role (security, finance, treasurer, ...) - null clears it. RESIDENT isn't a valid target. */
    @Transactional
    public ComplaintResponse assign(Long id, Role assignedRole) {
        if (assignedRole == Role.RESIDENT) {
            throw new BadRequestException("A complaint can't be assigned to the RESIDENT role");
        }
        Complaint complaint = get(id);
        complaint.setAssignedRole(assignedRole);
        complaint = complaintRepository.save(complaint);

        auditService.record("Complaint", complaint.getId(), "ASSIGN", assignedRole != null ? assignedRole.name() : "UNASSIGNED");
        return ComplaintResponse.from(complaint);
    }

    public ComplaintResponse findById(Long id) {
        return ComplaintResponse.from(get(id));
    }

    /**
     * {@code openOnly} mirrors ReportingService.dashboard's "open complaints" stat
     * (OPEN/ASSIGNED/IN_PROGRESS) so the dashboard card and this list agree on what "open"
     * means - it overrides a single {@code status} filter rather than combining with it.
     */
    public PageResponse<ComplaintResponse> search(Long residentId, ComplaintStatus status, boolean openOnly, Pageable pageable) {
        Specification<Complaint> spec = Specification.<Complaint>where(Specs.eq(residentId, "residentId"))
                .and(openOnly ? Specs.in(OPEN_STATUSES, "status") : Specs.eq(status, "status"));
        return PageResponse.of(complaintRepository.findAll(spec, pageable), ComplaintResponse::from);
    }

    private Complaint get(Long id) {
        return complaintRepository.findById(id).orElseThrow(() -> NotFoundException.of("Complaint", id));
    }
}
