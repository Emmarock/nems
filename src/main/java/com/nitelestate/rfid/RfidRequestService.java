package com.nitelestate.rfid;

import com.nitelestate.access.AccessSubjectType;
import com.nitelestate.audit.AuditService;
import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.common.exception.ConflictException;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.rfid.dto.RfidRequestCreateRequest;
import com.nitelestate.rfid.dto.RfidRequestResponse;
import com.nitelestate.rfid.dto.RfidTagRequest;
import com.nitelestate.rfid.dto.RfidTagResponse;
import com.nitelestate.visitor.Visitor;
import com.nitelestate.visitor.VisitorRepository;
import com.nitelestate.worker.Worker;
import com.nitelestate.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RfidRequestService {

    private final RfidRequestRepository rfidRequestRepository;
    private final RfidTagRepository rfidTagRepository;
    private final RfidService rfidService;
    private final ResidentRepository residentRepository;
    private final VisitorRepository visitorRepository;
    private final WorkerRepository workerRepository;
    private final AuditService auditService;

    /**
     * A resident requests a physical RFID tag for one of their own visitors or workers — never
     * for someone else's, same ownership check VisitorService.cancel/WorkerService.request
     * already apply. Only one PENDING request per subject at a time; the frontend hides the
     * button once a request exists, this is the server-side backstop.
     */
    @Transactional
    public RfidRequestResponse create(Long requestedByResidentId, RfidRequestCreateRequest request) {
        if (request.subjectType() != AccessSubjectType.VISITOR && request.subjectType() != AccessSubjectType.WORKER) {
            throw new BadRequestException("An RFID tag can only be requested for a visitor or a worker");
        }
        if (request.subjectType() == AccessSubjectType.VISITOR) {
            Visitor visitor = visitorRepository.findById(request.subjectId())
                    .orElseThrow(() -> NotFoundException.of("Visitor", request.subjectId()));
            if (!visitor.getHostResidentId().equals(requestedByResidentId)) {
                throw new BadRequestException("You can only request an RFID tag for a visitor you host");
            }
        } else {
            Worker worker = workerRepository.findById(request.subjectId())
                    .orElseThrow(() -> NotFoundException.of("Worker", request.subjectId()));
            if (!worker.getSponsorResidentId().equals(requestedByResidentId)) {
                throw new BadRequestException("You can only request an RFID tag for a worker you sponsor");
            }
        }
        if (rfidRequestRepository.findBySubjectTypeAndSubjectIdAndStatus(request.subjectType(), request.subjectId(),
                RfidRequestStatus.PENDING).isPresent()) {
            throw new ConflictException("An RFID request is already pending for this " + request.subjectType().toString().toLowerCase());
        }

        RfidRequest r = new RfidRequest();
        r.setRequestedByResidentId(requestedByResidentId);
        r.setSubjectType(request.subjectType());
        r.setSubjectId(request.subjectId());
        r.setStatus(RfidRequestStatus.PENDING);
        r = rfidRequestRepository.save(r);

        auditService.record("RfidRequest", r.getId(), "REQUEST", request.subjectType().toString());
        return toResponse(r);
    }

    public PageResponse<RfidRequestResponse> mine(Long residentId, Pageable pageable) {
        return PageResponse.of(rfidRequestRepository.findByRequestedByResidentId(residentId, pageable), this::toResponse);
    }

    /** Security's queue — every request still awaiting a physical tag. */
    public PageResponse<RfidRequestResponse> pending(Pageable pageable) {
        return PageResponse.of(rfidRequestRepository.findByStatus(RfidRequestStatus.PENDING, pageable), this::toResponse);
    }

    @Transactional
    public RfidRequestResponse cancel(Long id, Long residentId) {
        RfidRequest r = get(id);
        if (!r.getRequestedByResidentId().equals(residentId)) {
            throw new BadRequestException("You can only cancel your own RFID requests");
        }
        if (r.getStatus() != RfidRequestStatus.PENDING) {
            throw new BadRequestException("Only a pending RFID request can be cancelled");
        }
        r.setStatus(RfidRequestStatus.CANCELLED);
        r = rfidRequestRepository.save(r);
        auditService.record("RfidRequest", r.getId(), "CANCEL", null);
        return toResponse(r);
    }

    /**
     * Gate-side fulfilment: security hands the visitor/worker a physical tag and types its id
     * in here. Delegates the actual tag creation to RfidService.issue (same uniqueness/resident
     * resolution rules as issuing a tag directly), then marks this request ISSUED.
     */
    @Transactional
    public RfidRequestResponse issue(Long id, String tagId) {
        RfidRequest r = get(id);
        if (r.getStatus() != RfidRequestStatus.PENDING) {
            throw new BadRequestException("This RFID request is already " + r.getStatus().toString().toLowerCase());
        }

        RfidTagRequest tagRequest = r.getSubjectType() == AccessSubjectType.VISITOR
                ? new RfidTagRequest(tagId, null, null, r.getSubjectId(), null)
                : new RfidTagRequest(tagId, null, r.getSubjectId(), null, null);
        RfidTagResponse tag = rfidService.issue(tagRequest);

        r.setStatus(RfidRequestStatus.ISSUED);
        r.setIssuedTagId(tag.id());
        r = rfidRequestRepository.save(r);
        auditService.record("RfidRequest", r.getId(), "ISSUE", tagId);
        return toResponse(r);
    }

    private RfidRequest get(Long id) {
        return rfidRequestRepository.findById(id).orElseThrow(() -> NotFoundException.of("RfidRequest", id));
    }

    private RfidRequestResponse toResponse(RfidRequest r) {
        String requestedByName = residentRepository.findById(r.getRequestedByResidentId())
                .map(Resident::getFullName).orElse(null);

        String subjectName = null;
        String subjectPhone = null;
        if (r.getSubjectType() == AccessSubjectType.VISITOR) {
            Visitor v = visitorRepository.findById(r.getSubjectId()).orElse(null);
            if (v != null) {
                subjectName = v.getName();
                subjectPhone = v.getPhone();
            }
        } else {
            Worker w = workerRepository.findById(r.getSubjectId()).orElse(null);
            if (w != null) {
                subjectName = w.getFullName();
                subjectPhone = w.getPhone();
            }
        }

        String issuedTagTagId = r.getIssuedTagId() == null ? null
                : rfidTagRepository.findById(r.getIssuedTagId()).map(RfidTag::getTagId).orElse(null);

        return RfidRequestResponse.from(r, requestedByName, subjectName, subjectPhone, issuedTagTagId);
    }
}
