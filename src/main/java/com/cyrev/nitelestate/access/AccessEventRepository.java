package com.cyrev.nitelestate.access;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AccessEventRepository extends JpaRepository<AccessEvent, Long>, JpaSpecificationExecutor<AccessEvent> {

    /** Most recently recorded event for one subject, regardless of gate - used to enforce that
     * IN/OUT must alternate (see AccessEventService.record). Ordered by id, not occurredAt: id is
     * strictly monotonic on insert, occurredAt is wall-clock and could theoretically tie. */
    Optional<AccessEvent> findTopBySubjectTypeAndSubjectIdOrderByIdDesc(AccessSubjectType subjectType, Long subjectId);
}
