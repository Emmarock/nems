package com.nitelestate.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LevyRepository extends JpaRepository<Levy, Long> {
    List<Levy> findAllByActiveTrue();

    /** The levy StickerRequestService charges a sticker request against - see Levy.vehicleStickerLevy. */
    Optional<Levy> findFirstByVehicleStickerLevyTrueAndActiveTrue();

    /** The levies to auto-invoice on resident creation - see Levy.defaultForNewResident. */
    List<Levy> findAllByActiveTrueAndDefaultForNewResidentTrue();
}
