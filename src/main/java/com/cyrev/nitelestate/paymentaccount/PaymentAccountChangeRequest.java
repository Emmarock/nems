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

import java.time.Instant;

/**
 * A proposed change to one of the estate's payment accounts - only takes effect once two roles
 * other than the proposer's own have approved it (see PaymentAccountService.decide). Eligible
 * roles: SUPER_ADMIN, CDA_ADMIN, TREASURER, FINANCIAL_SECRETARY.
 *
 * targetAccountId null means this proposes creating a brand new account (e.g. a third one for a
 * new levy category) rather than editing an existing one - creating one is just as sensitive as
 * changing an existing one, so it goes through the same approval gate.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_account_change_request")
public class PaymentAccountChangeRequest extends BaseEntity {

    private Long targetAccountId;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false, length = 32)
    private String accountNumber;

    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false)
    private Long proposedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role proposedByRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChangeRequestStatus status = ChangeRequestStatus.PENDING;

    private Instant decidedAt;
}
