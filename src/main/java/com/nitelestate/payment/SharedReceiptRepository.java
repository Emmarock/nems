package com.nitelestate.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedReceiptRepository extends JpaRepository<SharedReceipt, Long> {
    Page<SharedReceipt> findByLandlordResidentIdAndStatus(Long landlordResidentId, SharedReceiptStatus status, Pageable pageable);
}
