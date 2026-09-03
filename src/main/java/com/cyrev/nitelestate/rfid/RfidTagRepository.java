package com.cyrev.nitelestate.rfid;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RfidTagRepository extends JpaRepository<RfidTag, Long> {
    Optional<RfidTag> findByTagId(String tagId);
    boolean existsByTagId(String tagId);
}
