package com.cyrev.nitelestate.payment.provider;

import java.math.BigDecimal;

/**
 * Provider abstraction from spec §5 so Paystack/Flutterwave/Monnify can be added later
 * without rewriting the finance module — just add a new implementation and switch
 * `nitel.payments.provider` in application.yml.
 */
public interface PaymentProvider {

    /** Provider key used to route webhook callbacks back to this implementation, e.g. "MOCK", "PAYSTACK". */
    String key();

    PaymentInitiationResult initiate(Long residentId, Long invoiceId, BigDecimal amount, String callbackReference);
}
