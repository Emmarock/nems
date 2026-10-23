package com.nitelestate.worker;

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
import com.nitelestate.notification.MessageTemplates;
import com.nitelestate.notification.NotificationChannel;
import com.nitelestate.notification.NotificationService;
import com.nitelestate.notification.UserNotificationService;
import com.nitelestate.property.Property;
import com.nitelestate.property.PropertyRepository;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.user.UserRepository;
import com.nitelestate.worker.dto.WorkerLookupResponse;
import com.nitelestate.worker.dto.WorkerRequest;
import com.nitelestate.worker.dto.WorkerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
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
    private final AccessPolicyService accessPolicyService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;
    private final UserRepository userRepository;

    /** ~1.5MB of base64 — comfortably covers a compressed photo (the frontend downscales before upload). */
    private static final int MAX_PHOTO_LENGTH = 2_000_000;

    /**
     * A resident (or the estate developer) requests worker access — issued immediately, same as
     * a Visitor pass, with no administrator sign-off gate. The site is always the sponsoring
     * resident's own property, resolved here rather than trusted from the request body — a
     * resident cannot request access for another home.
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
        worker.setWorkerCategory(request.workerCategory());
        worker.setSiteId(sponsor.getPropertyId());
        worker.setSponsorResidentId(sponsorResidentId);
        worker.setStartDate(request.startDate());
        worker.setExpectedEndDate(request.expectedEndDate());
        worker.setStatus(WorkerStatus.APPROVED);
        worker.setQrToken(UUID.randomUUID().toString());
        worker.setPhoto(request.photo());
        worker = workerRepository.save(worker);

        auditService.record("Worker", worker.getId(), "REQUEST", worker.getContractorName());
        return WorkerResponse.from(worker, resolveSiteHouseNumber(worker.getSiteId()));
    }

    /**
     * The sponsoring resident editing their own worker's basic details or photo after the
     * initial request — e.g. correcting a phone number, or adding a photo that wasn't on hand
     * at request time. Does not touch status/qrToken; a resident can't reassign the site either
     * (siteId is re-resolved from their own property, same as request()), so this can't be used
     * to move a worker onto someone else's property.
     */
    @Transactional
    public WorkerResponse updateMine(Long sponsorResidentId, Long id, WorkerRequest request) {
        Worker worker = get(id);
        if (!worker.getSponsorResidentId().equals(sponsorResidentId)) {
            throw new BadRequestException("That worker isn't yours to edit");
        }
        if (request.expectedEndDate().isBefore(request.startDate())) {
            throw new BadRequestException("expectedEndDate cannot be before startDate");
        }
        if (request.photo() != null && request.photo().length() > MAX_PHOTO_LENGTH) {
            throw new BadRequestException("Photo is too large — please use a smaller image");
        }

        worker.setFullName(request.fullName());
        worker.setPhone(request.phone());
        worker.setNationalId(request.nationalId());
        worker.setContractorName(request.contractorName());
        worker.setWorkType(request.workType());
        worker.setWorkerCategory(request.workerCategory());
        worker.setStartDate(request.startDate());
        worker.setExpectedEndDate(request.expectedEndDate());
        worker.setPhoto(request.photo());
        worker = workerRepository.save(worker);

        auditService.record("Worker", worker.getId(), "UPDATE", worker.getContractorName());
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
                worker.getContractorName(), worker.getWorkType(), worker.getWorkerCategory(), worker.getStartDate(), worker.getExpectedEndDate(),
                worker.getStatus(), flag, worker.getPhoto(),
                worker.getSponsorResidentId(), sponsor != null ? sponsor.getFullName() : null,
                sponsor != null ? sponsor.getPhone() : null,
                property != null ? property.getId() : null, property != null ? property.getHouseNumber() : null,
                property != null ? property.getAddress() : null);
    }

    /**
     * Gate-side check-in; evaluates the approval/date-range window and flags (not blocks)
     * anomalies there - but the sponsor's arrears status IS a hard block here (see
     * AccessPolicyService.requireSponsorEligibleForEntry); checking out is never denied by it.
     */
    @Transactional
    public WorkerResponse checkIn(String qrToken, Long gateId, Long verifiedByUserId) {
        Worker worker = workerRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No worker pass found for this QR code"));

        accessPolicyService.requireSponsorEligibleForEntry(worker.getSponsorResidentId());
        Evaluation eval = evaluate(worker);
        worker.setStatus(eval.impliedStatus());
        workerRepository.save(worker);

        accessEventService.record(AccessSubjectType.WORKER, worker.getId(), AccessDirection.IN, gateId,
                verifiedByUserId, eval.flag());
        auditService.record("Worker", worker.getId(), "CHECK_IN", eval.flag());
        notifySponsorOfScan(worker, AccessDirection.IN);
        return WorkerResponse.from(worker, resolveSiteHouseNumber(worker.getSiteId()));
    }

    @Transactional
    public WorkerResponse checkOut(String qrToken, Long gateId, Long verifiedByUserId) {
        Worker worker = workerRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No worker pass found for this QR code"));

        accessEventService.record(AccessSubjectType.WORKER, worker.getId(), AccessDirection.OUT, gateId,
                verifiedByUserId, null);
        auditService.record("Worker", worker.getId(), "CHECK_OUT", null);
        notifySponsorOfScan(worker, AccessDirection.OUT);
        return WorkerResponse.from(worker, resolveSiteHouseNumber(worker.getSiteId()));
    }

    /**
     * Lets the sponsoring resident know the moment their worker actually arrives or leaves —
     * in-app and SMS, same two channels either direction.
     */
    private void notifySponsorOfScan(Worker worker, AccessDirection direction) {
        Resident sponsor = residentRepository.findById(worker.getSponsorResidentId()).orElse(null);
        if (sponsor == null) {
            return;
        }
        boolean in = direction == AccessDirection.IN;
        String smsMessage = in
                ? MessageTemplates.workerCheckedIn(sponsor.getFullName(), worker.getFullName(), worker.getContractorName())
                : MessageTemplates.workerCheckedOut(sponsor.getFullName(), worker.getFullName(), worker.getContractorName());
        notificationService.dispatch(NotificationChannel.SMS, sponsor.getPhone(), smsMessage);

        String title = in ? "Worker checked in" : "Worker checked out";
        String message = worker.getFullName() + " (" + worker.getContractorName() + ") has just checked "
                + (in ? "in" : "out") + " at the gate.";
        userRepository.findByResidentId(sponsor.getId()).ifPresent(u -> userNotificationService.notify(u.getId(), title, message));
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

    /**
     * Admin filter: workers whose contract has reached (or passed) its end date but who are
     * still checked in - i.e. the same population CheckoutReminderJob emails landlords/sponsors
     * about, made browsable on demand. "Still checked in" can't be expressed as a single DB
     * predicate (it depends on each worker's latest access_event row), so this filters in
     * memory and paginates the already-small result by hand - the due-checkout population is
     * inherently bounded (everyone else has already left or been resolved), not the full roster.
     */
    public PageResponse<WorkerResponse> findDueCheckoutStillOnSite(Pageable pageable) {
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Lagos"));
        List<Worker> dueAndOnSite = workerRepository
                .findByStatusInAndExpectedEndDateLessThanEqual(List.of(WorkerStatus.APPROVED, WorkerStatus.ACTIVE), today)
                .stream()
                .filter(w -> accessEventService.isCurrentlyIn(AccessSubjectType.WORKER, w.getId()))
                // Longest-overdue first - the Pageable's own Sort isn't applicable here since the
                // underlying query can't express "still checked in" as a DB predicate (see above).
                .sorted(Comparator.comparing(Worker::getExpectedEndDate))
                .toList();

        int start = Math.min((int) pageable.getOffset(), dueAndOnSite.size());
        int end = Math.min(start + pageable.getPageSize(), dueAndOnSite.size());
        List<Worker> pageContent = dueAndOnSite.subList(start, end);

        List<Long> siteIds = pageContent.stream().map(Worker::getSiteId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> houseNumbers = propertyRepository.findAllById(siteIds).stream()
                .collect(Collectors.toMap(Property::getId, Property::getHouseNumber));
        List<WorkerResponse> content = pageContent.stream()
                .map(w -> WorkerResponse.from(w, houseNumbers.get(w.getSiteId())))
                .toList();

        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) dueAndOnSite.size() / pageable.getPageSize());
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), dueAndOnSite.size(), totalPages);
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
