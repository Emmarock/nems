package com.cyrev.nitelestate.paymentaccount;

import com.cyrev.nitelestate.common.BaseEntity;
import com.cyrev.nitelestate.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One role's decision on a PaymentAccountChangeRequest - see PaymentAccountService.decide for
 * why this is keyed by role, not just user (a second person holding the same role as an
 * existing decision, or as the proposer, can't cast an additional/duplicate sign-off). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_account_change_approval")
public class PaymentAccountChangeApproval extends BaseEntity {

    @Column(nullable = false)
    private Long changeRequestId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApprovalDecision decision;

    @Column(length = 500)
    private String notes;
}
