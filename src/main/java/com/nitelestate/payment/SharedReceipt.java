package com.nitelestate.payment;

import com.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A tenant can't submit a payment receipt directly (see PaymentService.submitReceipt) since
 * billing lives on the landlord's account, not theirs — this is what they hand over instead:
 * the landlord reviews it on their own Payments page and, if it's legitimate, submits it
 * themselves via PaymentService.submitSharedReceipt, which creates the real PENDING_APPROVAL
 * Payment exactly as if they'd filled the form in themselves.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shared_receipt")
public class SharedReceipt extends BaseEntity {

    @Column(nullable = false)
    private Long tenantResidentId;

    @Column(nullable = false)
    private Long landlordResidentId;

    @Column(nullable = false)
    private Long levyId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentMethod method;

    @Column(columnDefinition = "text")
    private String receiptImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SharedReceiptStatus status = SharedReceiptStatus.PENDING;
}
