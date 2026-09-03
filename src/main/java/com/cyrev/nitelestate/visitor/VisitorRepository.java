package com.cyrev.nitelestate.visitor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface VisitorRepository extends JpaRepository<Visitor, Long>, JpaSpecificationExecutor<Visitor> {
    Optional<Visitor> findByQrToken(String qrToken);
    long countByStatus(VisitorStatus status);
}
