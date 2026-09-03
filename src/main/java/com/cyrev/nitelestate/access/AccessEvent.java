package com.cyrev.nitelestate.access;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Unified entry/exit log, introduced in Phase 2 for visitor/worker check-in/out and extended
 * in Phase 3 with a real Gate reference and AccessPolicy flags — "Security does not maintain
 * its own resident/visitor/worker database" (spec Phase 3, architecture notes).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "access_event")
public class AccessEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccessSubjectType subjectType;

    @Column(nullable = false)
    private Long subjectId;

    /** Nullable until Phase 3's Gate module is registered against a physical gate. */
    private Long gateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private AccessDirection direction;

    @Column(nullable = false)
    private Instant occurredAt = Instant.now();

    /** User id of the security officer (or resident, for self check-in) who recorded this event. */
    private Long verifiedByUserId;

    /** Set when an AccessPolicy flagged this event, e.g. "ACCOUNT_IN_ARREARS", "PASS_EXPIRED". */
    private String flagReason;
}
