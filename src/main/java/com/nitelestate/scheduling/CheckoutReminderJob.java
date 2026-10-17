package com.nitelestate.scheduling;

import com.nitelestate.access.AccessEventService;
import com.nitelestate.access.AccessSubjectType;
import com.nitelestate.notification.MessageTemplates;
import com.nitelestate.notification.NotificationChannel;
import com.nitelestate.notification.NotificationService;
import com.nitelestate.property.Property;
import com.nitelestate.property.PropertyRepository;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.visitor.Visitor;
import com.nitelestate.visitor.VisitorRepository;
import com.nitelestate.visitor.VisitorStatus;
import com.nitelestate.worker.Worker;
import com.nitelestate.worker.WorkerCategory;
import com.nitelestate.worker.WorkerRepository;
import com.nitelestate.worker.WorkerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
 *    its end date. Checked every 5 minutes in the 18:20-22:00 estate-local window, but sends at
 *    most once per pass/contract — checkoutReminderSent marks it sent, since checkIn/checkOut
 *    never changes status (see WorkerService.checkOut) and nothing else would otherwise stop the
 *    query from matching the same overdue pass run after run.
 *
 * 2. remindLandlordsOfOvernightWorkers: a non-domestic-staff worker is still on site past the
 *    daily 6pm curfew, independent of how much contract time they have left - catches someone
 *    sleeping on site rather than leaving at the end of the work day. Fires once, shortly after
 *    6pm.
 *
 * 3. remindApproachingVisitorCheckouts: a real-time countdown to a visitor pass's own validUntil,
 *    at any time of day - starts 30 minutes before, re-fires every 10 minutes (so roughly three
 *    times: ~30, ~20, ~10 minutes out) and stops the moment validUntil passes. A friendly
 *    heads-up ahead of time, distinct from #1's "already due" sweep.
 *
 * 4. remindApproachingWorkerCurfew: the same 30-minute/10-minute countdown as #3, but anchored on
 *    the fixed 18:00 daily curfew instead of a per-visitor time - fires at 17:30/17:40/17:50.
 *    Complements #2's post-6pm "still here" catch rather than replacing it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutReminderJob {

    private static final List<WorkerStatus> ACTIVE_WORKER_STATUSES = List.of(WorkerStatus.APPROVED, WorkerStatus.ACTIVE);
    private static final ZoneId ESTATE_ZONE = ZoneId.of("Africa/Lagos");
    private static final LocalTime WINDOW_START = LocalTime.of(18, 20);
    private static final LocalTime WINDOW_END = LocalTime.of(22, 0);
    private static final LocalTime CURFEW = LocalTime.of(18, 0);
    private static final Duration APPROACHING_WINDOW = Duration.ofMinutes(30);

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
    @Transactional
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

    /**
     * Pre-departure countdown for visitors, distinct from remindLandlordsOfDueCheckouts above
     * (which only starts once a pass has reached its end date, and only runs in the 18:20-22:00
     * evening sweep). This one is real-time and applies to any checkout time of day: it fires
     * every 10 minutes, catching whichever ACTIVE visitors have validUntil falling in the next 30
     * minutes — since the job runs every 10 minutes and the window is 30 minutes wide, a given
     * visitor naturally gets caught ~3 times (roughly 30, 20, then 10 minutes out) and stops being
     * caught the moment validUntil passes, with no extra per-visitor state needed to track "have
     * we already reminded about this one" or how many times.
     */
    @Scheduled(cron = "0 */10 * * * *", zone = "Africa/Lagos")
    @Transactional(readOnly = true)
    public void remindApproachingVisitorCheckouts() {
        Instant now = Instant.now();
        List<Visitor> approaching = visitorRepository.findByStatusAndValidUntilBetween(
                VisitorStatus.ACTIVE, now, now.plus(APPROACHING_WINDOW));

        for (Visitor visitor : approaching) {
            long minutesLeft = roundToNearestTen(Duration.between(now, visitor.getValidUntil()).toMinutes());
            notifyLandlordAndSponsor(visitor.getHostResidentId(),
                    MessageTemplates.visitorCheckoutApproaching(visitor.getName(), minutesLeft));
        }
        if (!approaching.isEmpty()) {
            log.info("Approaching-checkout reminder: {} visitor(s) due within 30 minutes", approaching.size());
        }
    }

    /**
     * Pre-curfew countdown for non-domestic staff, same spirit as remindApproachingVisitorCheckouts
     * above but anchored on the fixed 18:00 curfew instead of a per-visitor validUntil, so it's
     * scheduled directly at 17:30/17:40/17:50 rather than needing a sliding-window query. A
     * friendly nudge ahead of time; remindLandlordsOfOvernightWorkers above remains the separate
     * post-6pm "they're still here" escalation for anyone who didn't leave in time.
     */
    @Scheduled(cron = "0 30,40,50 17 * * *", zone = "Africa/Lagos")
    @Transactional(readOnly = true)
    public void remindApproachingWorkerCurfew() {
        LocalDate today = LocalDate.now(ESTATE_ZONE);
        long minutesLeft = Duration.between(LocalTime.now(ESTATE_ZONE), CURFEW).toMinutes();
        List<Worker> withinContract = workerRepository.findByWorkerCategoryAndStatusInAndExpectedEndDateGreaterThanEqual(
                WorkerCategory.NON_DOMESTIC_STAFF, ACTIVE_WORKER_STATUSES, today);

        int flagged = 0;
        for (Worker worker : withinContract) {
            if (accessEventService.isCurrentlyIn(AccessSubjectType.WORKER, worker.getId())) {
                notifyLandlordAndSponsor(worker.getSponsorResidentId(),
                        MessageTemplates.workerCurfewApproaching(worker.getFullName(), worker.getContractorName(), minutesLeft));
                flagged++;
            }
        }
        log.info("Pre-curfew reminder: {} non-domestic worker(s) still on site, {} minute(s) before 6pm", flagged, minutesLeft);
    }

    private static long roundToNearestTen(long minutes) {
        return Math.max(Math.round(minutes / 10.0) * 10, 0);
    }

    private int remindForWorkers(LocalDate today) {
        List<Worker> due = workerRepository.findByStatusInAndExpectedEndDateLessThanEqualAndCheckoutReminderSentFalse(
                ACTIVE_WORKER_STATUSES, today);
        for (Worker worker : due) {
            notifyLandlordAndSponsor(worker.getSponsorResidentId(),
                    MessageTemplates.workerCheckoutDue(worker.getFullName(), worker.getContractorName(), worker.getExpectedEndDate()));
            worker.setCheckoutReminderSent(true);
            workerRepository.save(worker);
        }
        return due.size();
    }

    private int remindForVisitors(LocalDate today) {
        Instant endOfToday = today.plusDays(1).atStartOfDay(ESTATE_ZONE).toInstant();
        List<Visitor> due = visitorRepository.findByStatusAndValidUntilLessThanAndCheckoutReminderSentFalse(
                VisitorStatus.ACTIVE, endOfToday);
        for (Visitor visitor : due) {
            notifyLandlordAndSponsor(visitor.getHostResidentId(),
                    MessageTemplates.visitorCheckoutDue(visitor.getName(), visitor.getValidUntil()));
            visitor.setCheckoutReminderSent(true);
            visitorRepository.save(visitor);
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
