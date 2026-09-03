package com.cyrev.nitelestate.payment.dto;

public record OnlinePaymentInitiateResponse(Long paymentId, String providerReference, String redirectUrl) {
}
