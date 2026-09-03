package com.cyrev.nitelestate.payment.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stub standing in for Paystack/Flutterwave/Monnify (spec §5). Instead of calling a real
 * gateway, it hands back a link to the frontend's mock checkout page, which the resident
 * "pays" on and which then calls our own /payments/webhook — exercising the full
 * Resident -> Gateway -> Webhook -> NEMS -> Account Updated flow end-to-end without
 * needing live provider credentials.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {

    private final String frontendCheckoutUrl;

    public MockPaymentProvider(@Value("${nitel.payments.mock-checkout-url:http://localhost:5173/mock-checkout}") String frontendCheckoutUrl) {
        this.frontendCheckoutUrl = frontendCheckoutUrl;
    }

    @Override
    public String key() {
        return "MOCK";
    }

    @Override
    public PaymentInitiationResult initiate(Long residentId, Long invoiceId, BigDecimal amount, String callbackReference) {
        String providerReference = "MOCK-" + UUID.randomUUID();
        String redirectUrl = frontendCheckoutUrl + "?ref=" + providerReference + "&amount=" + amount;
        return new PaymentInitiationResult(providerReference, redirectUrl);
    }
}
