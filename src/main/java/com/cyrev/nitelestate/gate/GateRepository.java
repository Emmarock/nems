package com.cyrev.nitelestate.gate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GateRepository extends JpaRepository<Gate, Long> {
    boolean existsByCodeIgnoreCase(String code);
}
