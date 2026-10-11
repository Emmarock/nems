package com.cyrev.nitelestate.worker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long>, JpaSpecificationExecutor<Worker> {
    Optional<Worker> findByQrToken(String qrToken);
    long countByStatus(WorkerStatus status);

    /** Due-or-overdue checkouts still sitting in an active-ish status - see CheckoutReminderJob. */
    List<Worker> findByStatusInAndExpectedEndDateLessThanEqual(Collection<WorkerStatus> statuses, LocalDate date);
}
