package com.cyrev.nitelestate.sticker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StickerRequestRepository extends JpaRepository<StickerRequest, Long>, JpaSpecificationExecutor<StickerRequest> {
    boolean existsByVehicleId(Long vehicleId);
    Optional<StickerRequest> findByInvoiceId(Long invoiceId);
}
