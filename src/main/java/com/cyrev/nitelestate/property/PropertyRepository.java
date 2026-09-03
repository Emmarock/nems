package com.cyrev.nitelestate.property;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    boolean existsByHouseNumberIgnoreCase(String houseNumber);
    Optional<Property> findByQrToken(String qrToken);
}
