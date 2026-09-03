package com.cyrev.nitelestate.worker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long>, JpaSpecificationExecutor<Worker> {
    Optional<Worker> findByQrToken(String qrToken);
    long countByStatus(WorkerStatus status);
}
