package com.cyrev.nitelestate.payment;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Spec §5: payments recorded by admin staff (Phase 1) or via a PaymentProvider gateway (Phase 2). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private Long residentId;

    /** Optional: which invoice this payment is applied against. */
    private Long invoiceId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentMethod method;

    /** Provider key, e.g. "MOCK", "PAYSTACK". Null for manually recorded payments. */
    private String provider;

    /** Provider transaction reference, used to reconcile webhook callbacks. */
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.SUCCESS;

    @Column(nullable = false)
    private Instant paidAt = Instant.now();

    /** User id of the admin who recorded a manual payment; null for self-service online payments
     * and for resident-submitted receipts (see receiptImage) that haven't been reviewed yet. */
    private Long recordedByUserId;

    /** A resident's proof of an offline payment (e.g. a bank alert screenshot), submitted for
     * review - base64 data URI, size-capped in PaymentService, same convention as worker/visitor
     * photos. Null for admin-recorded and online-gateway payments, which need no such proof. */
    @Column(columnDefinition = "text")
    private String receiptImage;

    /** Who approved or rejected this payment - set for every SUCCESS/REJECTED payment that a
     * human actually decided on (manual entry: the recording staff member, who is in effect
     * approving it at the moment of entry; a submitted receipt: whoever reviewed it). Left null
     * for an online-gateway payment resolved by webhook - no human made that call. */
    private Long approvedByUserId;

    private Instant approvedAt;

    /** Optional remark from whoever approved/rejected - e.g. a rejection reason. */
    @Column(length = 500)
    private String reviewNotes;
}
