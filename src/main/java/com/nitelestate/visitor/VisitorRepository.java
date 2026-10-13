package com.nitelestate.visitor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VisitorRepository extends JpaRepository<Visitor, Long>, JpaSpecificationExecutor<Visitor> {
    Optional<Visitor> findByQrToken(String qrToken);
    long countByStatus(VisitorStatus status);

    /** Due-or-overdue checkouts still ACTIVE - see CheckoutReminderJob. */
    List<Visitor> findByStatusAndValidUntilLessThan(VisitorStatus status, Instant instant);
}
