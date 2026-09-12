package com.cyrev.nitelestate.payment;

public enum PaymentStatus {
    /** Awaiting an online-gateway webhook confirmation - resolved automatically, not by a human. */
    PENDING,
    /** A resident submitted a receipt for this payment; awaiting treasurer/financial-secretary review. */
    PENDING_APPROVAL,
    SUCCESS,
    FAILED,
    /** A submitted receipt was reviewed and rejected - the resident can resubmit against the same invoice. */
    REJECTED
}
