package com.nitelestate.property;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    boolean existsByHouseNumberIgnoreCase(String houseNumber);
    Optional<Property> findByQrToken(String qrToken);

    /** House numbers starting with the given (upper-case) prefix, e.g. "ROAD A-%" - see HouseNumbers.next. */
    @Query("select p.houseNumber from Property p where upper(p.houseNumber) like :prefix")
    List<String> findHouseNumbersLike(@Param("prefix") String prefix);

    /** The property/properties this resident owns — v1 assumes a landlord owns exactly one. */
    List<Property> findByOwnerId(Long ownerId);
}
