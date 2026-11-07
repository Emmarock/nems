package com.nitelestate.scheduling;

import com.nitelestate.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Every morning at 8am estate-local, emails the payment review offices (secretary, CDA admin,
 * treasurer, PRO, financial secretary) one digest of all receipts still awaiting approval, so
 * none are forgotten - including any submitted before office emails existed. Nothing is sent
 * when nothing is pending. See PaymentService.sendPendingDigest.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingPaymentDigestJob {

    private final PaymentService paymentService;

    @Scheduled(cron = "${nitel.payments.pending-digest-cron:0 0 8 * * *}", zone = "Africa/Lagos")
    public void sendDigest() {
        int pending = paymentService.sendPendingDigest();
        log.info("Pending payment digest: {} payment(s) awaiting approval", pending);
    }
}
