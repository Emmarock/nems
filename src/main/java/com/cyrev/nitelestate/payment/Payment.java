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
    @Column(nullable = false, length = 16)
    private PaymentStatus status = PaymentStatus.SUCCESS;

    @Column(nullable = false)
    private Instant paidAt = Instant.now();

    /** User id of the admin who recorded a manual payment; null for self-service online payments. */
    private Long recordedByUserId;
}
