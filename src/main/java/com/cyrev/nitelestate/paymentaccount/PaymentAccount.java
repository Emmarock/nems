package com.cyrev.nitelestate.paymentaccount;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One of the estate's bank accounts residents pay into - there isn't a single shared account:
 * different levies (e.g. Electricity, Development) are collected into different accounts, so a
 * Levy optionally links to one of these (see Levy.paymentAccountId). Changing an account, or
 * creating a new one, goes through PaymentAccountChangeRequest's multi-party approval rather
 * than being editable directly.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_account")
public class PaymentAccount extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String label = "";

    @Column(nullable = false)
    private String bankName = "";

    @Column(nullable = false, length = 32)
    private String accountNumber = "";

    @Column(nullable = false)
    private String accountName = "";

    @Column(nullable = false)
    private boolean active = true;
}
