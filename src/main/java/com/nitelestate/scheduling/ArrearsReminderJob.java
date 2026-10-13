package com.nitelestate.scheduling;

import com.nitelestate.notification.MessageTemplates;
import com.nitelestate.notification.NotificationChannel;
import com.nitelestate.notification.NotificationService;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.resident.ResidentService;
import com.nitelestate.resident.dto.ResidentArrearsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Every Saturday at 10am estate-local, emails each resident currently in arrears their own
 * outstanding balance - a standing nudge, independent of CheckoutReminderJob's worker/visitor
 * checkout reminders. Reuses the exact same "in arrears" population as the Accounts in Arrears
 * page and its downloadable PDF report (ResidentService.allInArrears), so this job, that page,
 * and that report never disagree about who owes what. Unlike CheckoutReminderJob's notifications
 * (sponsor/landlord, because someone else's pass is at stake), this goes straight to the debtor —
 * it's their own balance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArrearsReminderJob {

    private final ResidentService residentService;
    private final ResidentRepository residentRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 10 * * SAT", zone = "Africa/Lagos")
    @Transactional(readOnly = true)
    public void remindResidentsInArrears() {
        List<ResidentArrearsResponse> debtors = residentService.allInArrears();
        int emailed = 0;
        for (ResidentArrearsResponse debtor : debtors) {
            Resident resident = residentRepository.findById(debtor.id()).orElse(null);
            if (resident == null || resident.getEmail() == null || resident.getEmail().isBlank()) {
                continue;
            }
            String message = MessageTemplates.arrearsReminder(resident.getFullName(), debtor.outstanding());
            notificationService.dispatch(NotificationChannel.EMAIL, resident.getEmail(), message, "Outstanding balance reminder");
            emailed++;
        }
        log.info("Weekly arrears reminder: emailed {} of {} account(s) in arrears", emailed, debtors.size());
    }
}
