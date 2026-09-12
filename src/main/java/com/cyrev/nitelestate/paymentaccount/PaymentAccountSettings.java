package com.cyrev.nitelestate.paymentaccount;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single configurable settings row (same pattern as AccessPolicySettings) - the one bank
 * account residents should pay any levy into. There's no per-levy account: the estate collects
 * everything through this one account and reconciles by the payment receipt a resident submits
 * afterwards (see PaymentService.submitReceipt).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_account_settings")
public class PaymentAccountSettings extends BaseEntity {

    @Column(nullable = false)
    private String bankName = "";

    @Column(nullable = false, length = 32)
    private String accountNumber = "";

    @Column(nullable = false)
    private String accountName = "";
}
