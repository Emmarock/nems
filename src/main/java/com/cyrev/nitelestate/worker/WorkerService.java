package com.cyrev.nitelestate.worker;

import com.cyrev.nitelestate.access.AccessDirection;
import com.cyrev.nitelestate.access.AccessEventService;
import com.cyrev.nitelestate.access.AccessSubjectType;
import com.cyrev.nitelestate.access.dto.AccessEventResponse;
import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.property.Property;
import com.cyrev.nitelestate.property.PropertyRepository;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.worker.dto.WorkerLookupResponse;
import com.cyrev.nitelestate.worker.dto.WorkerRequest;
import com.cyrev.nitelestate.worker.dto.WorkerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final ResidentRepository residentRepository;
    private final PropertyRepository propertyRepository;
    private final AccessEventService accessEventService;
    private final AuditService auditService;

    /** ~1.5MB of base64 — comfortably covers a compressed photo (the frontend downscales before upload). */
    private static final int MAX_PHOTO_LENGTH = 2_000_000;

    /**
     * A resident (or the estate developer) requests worker access — starts as PENDING (spec §4).
     * The site is always the sponsoring resident's own property, resolved here rather than
     * trusted from the request body — a resident cannot request access for another home.
     */
    @Transactional
    public WorkerResponse request(Long sponsorResidentId, WorkerRequest request) {
        if (request.expectedEndDate().isBefore(request.startDate())) {
            throw new BadRequestException("expectedEndDate cannot be before startDate");
        }
        if (request.photo() != null && request.photo().length() > MAX_PHOTO_LENGTH) {
            throw new BadRequestException("Photo is too large — please use a smaller image");
        }
        Resident sponsor = residentRepository.findById(sponsorResidentId)
                .orElseThrow(() -> NotFoundException.of("Resident", sponsorResidentId));

        Worker worker = new Worker();
        worker.setFullName(request.fullName());
        worker.setPhone(request.phone());
        worker.setNationalId(request.nationalId());
        worker.setContractorName(request.contractorName());
        worker.setWorkType(request.workType());
        worker.setSiteId(sponsor.getPropertyId());
        worker.setSponsorResidentId(sponsorResidentId);
        worker.setStartDate(request.startDate());
        worker.setExpectedEndDate(request.expectedEndDate());
        worker.setStatus(WorkerStatus.PENDING);
        worker.setPhoto(request.photo());
        worker = workerRepository.save(worker);

        auditService.record("Worker", worker.getId(), "REQUEST", worker.getContractorName());
        return WorkerResponse.from(worker, resolveSiteHouseNumber(worker.getSiteId()));
    }

    /** CDA Administrator sign-off (spec §4 approval workflow); issues the QR access pass. */
    @Transactional
    public WorkerResponse approve(Long id) {
        Worker worker = get(id);
        if (worker.getStatus() != WorkerStatus.PENDING) {
            throw new BadRequestException("Only PENDING worker requests can be approved");
        }
        worker.setStatus(WorkerStatus.APPROVED);
        worker.setQrToken(UUID.randomUUID().toString());
        worker = workerRepository.save(worker);

        auditService.record("Worker", worker.getId(), "APPROVE", null);
        return WorkerResponse.from(worker, resolveSiteHouseNumber(worker.getSiteId()));
    }

    @Transactional
    public WorkerResponse suspend(Long id) {
        Worker worker = get(id);
        worker.setStatus(WorkerStatus.SUSPENDED);
        worker = workerRepository.save(worker);
        auditService.record("Worker", worker.getId(), "SUSPEND", null);
        return WorkerResponse.from(worker, resolveSiteHouseNumber(worker.getSiteId()));
    }

    @Transactional
    public WorkerResponse complete(Long id) {
        Worker worker = get(id);
        worker.setStatus(WorkerStatus.COMPLETED);
        worker = workerRepository.save(worker);
        auditService.record("Worker", worker.getId(), "COMPLETE", null);
        return WorkerResponse.from(worker, resolveSiteHouseNumber(worker.getSiteId()));
    }

    /**
     * Read-only gate-side scan: resolves the pass holder's destination (sponsor + property) so
     * security can confirm it before deciding whether to check the worker in. Does not mutate
     * worker status or write an access event — see checkIn for that.
     */
    public WorkerLookupResponse lookup(String qrToken) {
        Worker worker = workerRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No worker pass found for this QR code"));
        String flag = evaluate(worker).flag();

        Resident sponsor = residentRepository.findById(worker.getSponsorResidentId()).orElse(null);
        Property property = worker.getSiteId() != null ? propertyRepository.findById(worker.getSiteId()).orElse(null) : null;

        return new WorkerLookupResponse(worker.getId(), worker.getFullName(), worker.getPhone(),
                worker.getContractorName(), worker.getWorkType(), worker.getStartDate(), worker.getExpectedEndDate(),
                worker.getStatus(), flag, worker.getPhoto(),
                worker.getSponsorResidentId(), sponsor != null ? sponsor.getFullName() : null,
                sponsor != null ? sponsor.getPhone() : null,
                property != null ? property.getId() : null, property != null ? property.getHouseNumber() : null,
                property != null ? property.getAddress() : null);
    }

    /** Gate-side check-in; evaluates the approval/date-range window and flags (not blocks) anomalies. */
    @Transactional
    public WorkerResponse checkIn(String qrToken, Long gateId, Long verifiedByUserId) {
        Worker worker = workerRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No worker pass found for this QR code"));

        Evaluation eval = evaluate(worker);
        worker.setStatus(eval.impliedStatus());
        workerRepository.save(worker);

        accessEventService.record(AccessSubjectType.WORKER, worker.getId(), AccessDirection.IN, gateId,
                verifiedByUserId, eval.flag());
        auditService.record("Worker", worker.getId(), "CHECK_IN", eval.flag());
        return WorkerResponse.from(worker, resolveSiteHouseNumber(worker.getSiteId()));
    }

    @Transactional
    public WorkerResponse checkOut(String qrToken, Long gateId, Long verifiedByUserId) {
        Worker worker = workerRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No worker pass found for this QR code"));

        accessEventService.record(AccessSubjectType.WORKER, worker.getId(), AccessDirection.OUT, gateId,
                verifiedByUserId, null);
        auditService.record("Worker", worker.getId(), "CHECK_OUT", null);
        return WorkerResponse.from(worker, resolveSiteHouseNumber(worker.getSiteId()));
    }

    public PageResponse<AccessEventResponse> logs(Long workerId, Pageable pageable) {
        return accessEventService.search(AccessSubjectType.WORKER, workerId, null, pageable);
    }

    public WorkerResponse findById(Long id) {
        Worker worker = get(id);
        return WorkerResponse.from(worker, resolveSiteHouseNumber(worker.getSiteId()));
    }

    public PageResponse<WorkerResponse> search(String q, Long sponsorResidentId, boolean activeOnly, Pageable pageable) {
        Specification<Worker> spec = Specification.<Worker>where(Specs.contains(q, "fullName", "contractorName", "workType"))
                .and(Specs.eq(sponsorResidentId, "sponsorResidentId"))
                .and(activeOnly ? Specs.eq(WorkerStatus.ACTIVE, "status") : null);
        var page = workerRepository.findAll(spec, pageable);

        List<Long> siteIds = page.getContent().stream().map(Worker::getSiteId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> houseNumbers = propertyRepository.findAllById(siteIds).stream()
                .collect(Collectors.toMap(Property::getId, Property::getHouseNumber));

        return PageResponse.of(page.map(w -> WorkerResponse.from(w, houseNumbers.get(w.getSiteId()))));
    }

    private String resolveSiteHouseNumber(Long siteId) {
        return siteId == null ? null : propertyRepository.findById(siteId).map(Property::getHouseNumber).orElse(null);
    }

    private record Evaluation(String flag, WorkerStatus impliedStatus) {
    }

    /** Pure evaluation of PENDING/APPROVED/ACTIVE state vs today's date window — no mutation. */
    private Evaluation evaluate(Worker worker) {
        LocalDate today = LocalDate.now();

        if (worker.getStatus() == WorkerStatus.SUSPENDED) {
            return new Evaluation("WORKER_SUSPENDED", worker.getStatus());
        }
        if (worker.getStatus() == WorkerStatus.COMPLETED) {
            return new Evaluation("WORKER_COMPLETED", worker.getStatus());
        }
        if (worker.getStatus() == WorkerStatus.PENDING) {
            return new Evaluation("NOT_APPROVED", worker.getStatus());
        }
        if (today.isAfter(worker.getExpectedEndDate())) {
            return new Evaluation("PASS_EXPIRED", WorkerStatus.EXPIRED);
        }
        if (today.isBefore(worker.getStartDate())) {
            return new Evaluation("BEFORE_START_DATE", worker.getStatus());
        }
        // APPROVED or ACTIVE, within the date range: (re)activate.
        return new Evaluation(null, WorkerStatus.ACTIVE);
    }

    private Worker get(Long id) {
        return workerRepository.findById(id).orElseThrow(() -> NotFoundException.of("Worker", id));
    }
}
