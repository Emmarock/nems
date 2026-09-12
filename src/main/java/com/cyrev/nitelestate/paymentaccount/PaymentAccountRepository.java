package com.cyrev.nitelestate.paymentaccount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Long> {
    List<PaymentAccount> findAllByActiveTrue();
}
