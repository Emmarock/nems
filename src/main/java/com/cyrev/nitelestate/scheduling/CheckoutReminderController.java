package com.cyrev.nitelestate.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Manual "run it now" triggers for CheckoutReminderJob — same work the crons do, for testing or an ad-hoc nudge. */
@RestController
@RequestMapping("/api/v1/admin/checkout-reminders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CheckoutReminderController {

    private final CheckoutReminderJob checkoutReminderJob;

    @PostMapping("/run")
    public void run() {
        checkoutReminderJob.remindLandlordsOfDueCheckouts();
    }

    @PostMapping("/run-overnight-check")
    public void runOvernightCheck() {
        checkoutReminderJob.remindLandlordsOfOvernightWorkers();
    }
}
