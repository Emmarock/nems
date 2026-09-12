package com.cyrev.nitelestate.paymentaccount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentAccountChangeRequestRepository extends JpaRepository<PaymentAccountChangeRequest, Long> {
    Optional<PaymentAccountChangeRequest> findFirstByStatusOrderByCreatedAtDesc(ChangeRequestStatus status);
}
