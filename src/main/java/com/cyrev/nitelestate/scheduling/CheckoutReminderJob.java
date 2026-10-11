package com.cyrev.nitelestate.scheduling;

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
import java.util.List;

/**
 * Reminds a property's landlord when a worker/visitor they (or one of their tenants) sponsored
 * is due - or overdue - to check out, so the landlord can chase them to wrap up and leave. Goes
 * to the landlord specifically, even when a tenant is the one who actually sponsored the pass -
 * the landlord is the property's accountable contact, same reasoning as every other
 * landlord-oversight feature in this app (tenant vehicle approval, apartment-count cap, etc.).
 *
 * Fires every 5 minutes in the 18:20-22:00 estate-local window every day (an evening check-out
 * nudge, not an all-day one) - see the @Scheduled cron below for why that's split between the
 * cron expression and an in-method guard. Deliberately re-fires every run a pass stays active
 * past its due date rather than sending once and going quiet - checkIn/checkOut never changes a
 * Worker/Visitor's status (see WorkerService.checkOut), so nothing else would ever stop flagging
 * an overdue pass otherwise. It stops only once the record is actually resolved (worker marked
 * COMPLETED/SUSPENDED, or a visitor pass CANCELLED) - an admin/security action, not something
 * this job does itself.
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

    private int remindForWorkers(LocalDate today) {
        List<Worker> due = workerRepository.findByStatusInAndExpectedEndDateLessThanEqual(ACTIVE_WORKER_STATUSES, today);
        for (Worker worker : due) {
            notifyLandlord(worker.getSponsorResidentId(),
                    MessageTemplates.workerCheckoutDue(worker.getFullName(), worker.getContractorName(), worker.getExpectedEndDate()));
        }
        return due.size();
    }

    private int remindForVisitors(LocalDate today) {
        Instant endOfToday = today.plusDays(1).atStartOfDay(ESTATE_ZONE).toInstant();
        List<Visitor> due = visitorRepository.findByStatusAndValidUntilLessThan(VisitorStatus.ACTIVE, endOfToday);
        for (Visitor visitor : due) {
            notifyLandlord(visitor.getHostResidentId(),
                    MessageTemplates.visitorCheckoutDue(visitor.getName(), visitor.getValidUntil()));
        }
        return due.size();
    }

    /**
     * Resolves sponsorResidentId -> their property -> that property's landlord, then dispatches
     * to the landlord's own email/phone (not the sponsor's, if the sponsor is a tenant). Silently
     * does nothing if any link in that chain is missing (no property, no landlord on file, no
     * contact details) - there's no one meaningful to notify, not a failure worth surfacing.
     */
    private void notifyLandlord(Long sponsorResidentId, String message) {
        Resident sponsor = residentRepository.findById(sponsorResidentId).orElse(null);
        if (sponsor == null || sponsor.getPropertyId() == null) {
            return;
        }
        Property property = propertyRepository.findById(sponsor.getPropertyId()).orElse(null);
        if (property == null || property.getOwnerId() == null) {
            return;
        }
        Resident landlord = residentRepository.findById(property.getOwnerId()).orElse(null);
        if (landlord == null) {
            return;
        }
        if (landlord.getEmail() != null && !landlord.getEmail().isBlank()) {
            notificationService.dispatch(NotificationChannel.EMAIL, landlord.getEmail(), message);
        }
        // Mobile-app push isn't wired up yet - there's no mobile app in this codebase to receive
        // one. See the conversation for the options; SMS/WhatsApp were dropped here on purpose
        // since the request named email + push specifically, not those two.
    }
}
