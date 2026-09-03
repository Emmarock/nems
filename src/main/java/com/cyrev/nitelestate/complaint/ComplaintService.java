package com.cyrev.nitelestate.complaint;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.complaint.dto.ComplaintRequest;
import com.cyrev.nitelestate.complaint.dto.ComplaintResponse;
import com.cyrev.nitelestate.complaint.dto.ComplaintUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final AuditService auditService;

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
        if (request.assignedTo() != null) {
            complaint.setAssignedTo(request.assignedTo());
        }
        if (request.status() == ComplaintStatus.RESOLVED || request.status() == ComplaintStatus.CLOSED) {
            complaint.setResolvedAt(Instant.now());
        }
        complaint = complaintRepository.save(complaint);

        auditService.record("Complaint", complaint.getId(), "STATUS_" + request.status(), request.assignedTo());
        return ComplaintResponse.from(complaint);
    }

    public ComplaintResponse findById(Long id) {
        return ComplaintResponse.from(get(id));
    }

    public PageResponse<ComplaintResponse> search(Long residentId, ComplaintStatus status, Pageable pageable) {
        Specification<Complaint> spec = Specification.<Complaint>where(Specs.eq(residentId, "residentId"))
                .and(Specs.eq(status, "status"));
        return PageResponse.of(complaintRepository.findAll(spec, pageable), ComplaintResponse::from);
    }

    private Complaint get(Long id) {
        return complaintRepository.findById(id).orElseThrow(() -> NotFoundException.of("Complaint", id));
    }
}
