package com.cyrev.nitelestate.paymentaccount;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAccountChangeApprovalRepository extends JpaRepository<PaymentAccountChangeApproval, Long> {
    List<PaymentAccountChangeApproval> findByChangeRequestId(Long changeRequestId);
}
