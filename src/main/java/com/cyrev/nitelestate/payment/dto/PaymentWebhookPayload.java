package com.cyrev.nitelestate.payment.dto;

import jakarta.validation.constraints.NotBlank;

/** Simulates a real gateway's webhook callback shape (spec §5: Payment Gateway -> Webhook -> NEMS). */
public record PaymentWebhookPayload(
        @NotBlank String providerReference,
        @NotBlank String status
) {
}
