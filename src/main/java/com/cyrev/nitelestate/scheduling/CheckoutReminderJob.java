package com.cyrev.nitelestate.scheduling;

import com.cyrev.nitelestate.access.AccessEventService;
import com.cyrev.nitelestate.access.AccessSubjectType;
import com.cyrev.nitelestate.notification.MessageTemplates;
import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationService;
import com.cyrev.nitelestate.property.Property;
import com.cyrev.nitelestate.property.PropertyRepository;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.visitor.Visitor;
import com.cyrev.nitelestate.visitor.VisitorRepository;
import com.cyrev.nitelestate.visitor.VisitorStatus;
import com.cyrev.nitelestate.worker.Worker;
import com.cyrev.nitelestate.worker.WorkerCategory;
import com.cyrev.nitelestate.worker.WorkerRepository;
import com.cyrev.nitelestate.worker.WorkerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Two related reminders, both emailing the sponsor (e.g. the tenant who actually requested the
 * worker/visitor, and can reach them directly) AND the property's landlord (the accountable
 * contact, resolved via sponsor -> property -> owner - same reasoning as every other
 * landlord-oversight feature in this app: tenant vehicle approval, apartment-count cap, etc.).
 * The two are deduplicated by email address, so a landlord sponsoring their own worker/visitor
 * gets one email, not two.
 *
 * 1. remindLandlordsOfDueCheckouts: a worker/visitor's own contract/pass has reached (or passed)
 *    its end date. Fires every 5 minutes in the 18:20-22:00 estate-local window, re-firing every
 *    run a pass stays active past its due date rather than going quiet after one send -
 *    checkIn/checkOut never changes status (see WorkerService.checkOut), so nothing else would
 *    ever stop flagging an overdue pass otherwise. Stops only once actually resolved (worker
 *    marked COMPLETED/SUSPENDED, visitor pass CANCELLED).
 *
 * 2. remindLandlordsOfOvernightWorkers: a non-domestic-staff worker is still on site past the
 *    daily 6pm curfew, independent of how much contract time they have left - catches someone
 *    sleeping on site rather than leaving at the end of the work day. Fires once, shortly after
 *    6pm.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutReminderJob {

    private static final List<WorkerStatus> ACTIVE_WORKER_STATUSES = List.of(WorkerStatus.APPROVED, WorkerStatus.ACTIVE);
    private static final ZoneId ESTATE_ZONE = ZoneId.of("Africa/Lagos");
    private static final LocalTime WINDOW_START = LocalTime.of(18, 20);
    private static final LocalTime WINDOW_END = LocalTime.of(22, 0);

    private final WorkerRepository workerRepository;
    private final VisitorRepository visitorRepository;
    private final ResidentRepository residentRepository;
    private final PropertyRepository propertyRepository;
    private final AccessEventService accessEventService;
    private final NotificationService notificationService;

    /**
     * Cron fires every 5 minutes across hours 18-21 (estate-local, regardless of the server's own
     * timezone - e.g. Render runs UTC); the in-method guard below trims that to the exact
     * 18:20-22:00 window, since cron's hour/minute fields can't express "start at :20" on their
     * own without a separate trigger per partial hour.
     */
    @Scheduled(cron = "0 */5 18-21 * * *", zone = "Africa/Lagos")
    @Transactional(readOnly = true)
    public void remindLandlordsOfDueCheckouts() {
        LocalTime now = LocalTime.now(ESTATE_ZONE);
        if (now.isBefore(WINDOW_START) || now.isAfter(WINDOW_END)) {
            return;
        }
        LocalDate today = LocalDate.now(ESTATE_ZONE);
        int workerCount = remindForWorkers(today);
        int visitorCount = remindForVisitors(today);
        log.info("Checkout reminder job: {} worker(s), {} visitor(s) flagged for {}", workerCount, visitorCount, today);
    }

    /**
     * Non-domestic staff (contractors/artisans) are expected to check out every day by 6pm, even
     * when their contract has days left - this is what catches someone sleeping on site rather
     * than leaving at the end of the work day. Fires once, shortly after 6pm (not repeatedly like
     * remindLandlordsOfDueCheckouts above, which is about the contract's own end date, not a
     * daily curfew). "Still on site" means their most recent WORKER access event is IN - entries
     * and exits strictly alternate (see AccessEventService.record), so that's enough to tell
     * without needing to filter by when the IN happened.
     */
    @Scheduled(cron = "0 5 18 * * *", zone = "Africa/Lagos")
    @Transactional(readOnly = true)
    public void remindLandlordsOfOvernightWorkers() {
        LocalDate today = LocalDate.now(ESTATE_ZONE);
        List<Worker> withinContract = workerRepository.findByWorkerCategoryAndStatusInAndExpectedEndDateGreaterThanEqual(
                WorkerCategory.NON_DOMESTIC_STAFF, ACTIVE_WORKER_STATUSES, today);

        int flagged = 0;
        for (Worker worker : withinContract) {
            if (accessEventService.isCurrentlyIn(AccessSubjectType.WORKER, worker.getId())) {
                notifyLandlordAndSponsor(worker.getSponsorResidentId(),
                        MessageTemplates.workerPastDailyCurfew(worker.getFullName(), worker.getContractorName()));
                flagged++;
            }
        }
        log.info("Daily on-site curfew check: {} non-domestic worker(s) still checked in past 6pm", flagged);
    }

    private int remindForWorkers(LocalDate today) {
        List<Worker> due = workerRepository.findByStatusInAndExpectedEndDateLessThanEqual(ACTIVE_WORKER_STATUSES, today);
        for (Worker worker : due) {
            notifyLandlordAndSponsor(worker.getSponsorResidentId(),
                    MessageTemplates.workerCheckoutDue(worker.getFullName(), worker.getContractorName(), worker.getExpectedEndDate()));
        }
        return due.size();
    }

    private int remindForVisitors(LocalDate today) {
        Instant endOfToday = today.plusDays(1).atStartOfDay(ESTATE_ZONE).toInstant();
        List<Visitor> due = visitorRepository.findByStatusAndValidUntilLessThan(VisitorStatus.ACTIVE, endOfToday);
        for (Visitor visitor : due) {
            notifyLandlordAndSponsor(visitor.getHostResidentId(),
                    MessageTemplates.visitorCheckoutDue(visitor.getName(), visitor.getValidUntil()));
        }
        return due.size();
    }

    /**
     * Emails both the sponsor (who actually requested the pass - e.g. a tenant, who can reach
     * their own staff/guest directly) and the property's landlord (the accountable contact,
     * resolved via sponsor -> property -> owner). Deduplicated by address, so a landlord
     * sponsoring their own worker/visitor - sponsor and landlord are the same resident - gets
     * exactly one email, not two. Silently skips anyone with no email on file or no landlord
     * link; there's no one meaningful to notify in that gap, not a failure worth surfacing.
     */
    private void notifyLandlordAndSponsor(Long sponsorResidentId, String message) {
        Resident sponsor = residentRepository.findById(sponsorResidentId).orElse(null);
        if (sponsor == null) {
            return;
        }
        Set<String> emailed = new HashSet<>();
        emailIfNew(sponsor, message, emailed);

        if (sponsor.getPropertyId() == null) {
            return;
        }
        Property property = propertyRepository.findById(sponsor.getPropertyId()).orElse(null);
        if (property == null || property.getOwnerId() == null) {
            return;
        }
        Resident landlord = residentRepository.findById(property.getOwnerId()).orElse(null);
        if (landlord != null) {
            emailIfNew(landlord, message, emailed);
        }
    }

    private void emailIfNew(Resident resident, String message, Set<String> alreadyEmailed) {
        String email = resident.getEmail();
        if (email != null && !email.isBlank() && alreadyEmailed.add(email.toLowerCase())) {
            notificationService.dispatch(NotificationChannel.EMAIL, email, message);
        }
        // Mobile-app push isn't wired up yet - there's no mobile app in this codebase to receive
        // one. See the conversation for the options; SMS/WhatsApp were dropped here on purpose
        // since an earlier request named email + push specifically, not those two.
    }
}
