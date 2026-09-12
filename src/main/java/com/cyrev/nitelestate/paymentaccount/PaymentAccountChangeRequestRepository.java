package com.cyrev.nitelestate.paymentaccount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentAccountChangeRequestRepository extends JpaRepository<PaymentAccountChangeRequest, Long> {
    List<PaymentAccountChangeRequest> findAllByStatus(ChangeRequestStatus status);

    /** null targetAccountId means "proposing a brand new account" - at most one such proposal
     * pending at a time, same as at most one pending edit per existing account. */
    Optional<PaymentAccountChangeRequest> findFirstByTargetAccountIdIsNullAndStatus(ChangeRequestStatus status);

    Optional<PaymentAccountChangeRequest> findFirstByTargetAccountIdAndStatus(Long targetAccountId, ChangeRequestStatus status);
}
