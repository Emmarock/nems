package com.cyrev.nitelestate.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual "run it now" triggers for the app's scheduled reminder jobs — same work the crons do,
 * for testing or an ad-hoc nudge. Covers both CheckoutReminderJob and ArrearsReminderJob; kept on
 * one controller since both are small, SUPER_ADMIN-only, fire-and-forget triggers of the same kind.
 */
@RestController
@RequestMapping("/api/v1/admin/checkout-reminders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CheckoutReminderController {

    private final CheckoutReminderJob checkoutReminderJob;
    private final ArrearsReminderJob arrearsReminderJob;

    @PostMapping("/run")
    public void run() {
        checkoutReminderJob.remindLandlordsOfDueCheckouts();
    }

    @PostMapping("/run-overnight-check")
    public void runOvernightCheck() {
        checkoutReminderJob.remindLandlordsOfOvernightWorkers();
    }

    @PostMapping("/run-arrears-reminder")
    public void runArrearsReminder() {
        arrearsReminderJob.remindResidentsInArrears();
    }
}
