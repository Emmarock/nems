package com.nitelestate.rfid;

import com.nitelestate.access.AccessSubjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RfidRequestRepository extends JpaRepository<RfidRequest, Long> {
    Page<RfidRequest> findByStatus(RfidRequestStatus status, Pageable pageable);
    Page<RfidRequest> findByRequestedByResidentId(Long requestedByResidentId, Pageable pageable);
    Optional<RfidRequest> findBySubjectTypeAndSubjectIdAndStatus(AccessSubjectType subjectType, Long subjectId, RfidRequestStatus status);
}
