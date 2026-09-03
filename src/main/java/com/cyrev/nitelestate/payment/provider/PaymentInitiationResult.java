package com.cyrev.nitelestate.payment.provider;

/** What a PaymentProvider returns after starting an online payment (spec §5). */
public record PaymentInitiationResult(String providerReference, String redirectUrl) {
}
