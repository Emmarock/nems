package com.cyrev.nitelestate.paymentaccount.dto;

/** Notes optional for either an approval or a rejection - not enforced either way. */
public record PaymentAccountDecisionRequest(String notes) {
}
