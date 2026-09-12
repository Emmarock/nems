package com.cyrev.nitelestate.payment.dto;

/** Treasurer/financial-secretary decision on a PENDING_APPROVAL payment. notes is optional for an
 * approval, a good idea for a rejection (why), but not enforced either way. */
public record PaymentReviewRequest(String notes) {
}
