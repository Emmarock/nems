package com.cyrev.nitelestate.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LevyRepository extends JpaRepository<Levy, Long> {
    List<Levy> findAllByActiveTrue();
}
