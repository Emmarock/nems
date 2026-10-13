package com.nitelestate.property;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    boolean existsByHouseNumberIgnoreCase(String houseNumber);
    Optional<Property> findByQrToken(String qrToken);

    /** The property/properties this resident owns — v1 assumes a landlord owns exactly one. */
    List<Property> findByOwnerId(Long ownerId);
}
