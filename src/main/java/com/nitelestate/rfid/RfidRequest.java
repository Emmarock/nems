package com.nitelestate.rfid;

import com.nitelestate.access.AccessSubjectType;
import com.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A landlord/tenant's request for a physical RFID credential on behalf of a visitor or worker
 * who has no smartphone to show a QR pass — e.g. an elderly relative or a domestic worker.
 * Reuses AccessSubjectType rather than a new enum; only VISITOR and WORKER are valid here
 * (enforced in RfidRequestService, not by the type itself). Security sees every PENDING request
 * and fulfils it at the gate by entering a physical tag id, which both creates the real
 * {@link RfidTag} (via RfidService.issue) and sets issuedTagId here.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rfid_request")
public class RfidRequest extends BaseEntity {

    @Column(nullable = false)
    private Long requestedByResidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccessSubjectType subjectType;

    @Column(nullable = false)
    private Long subjectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RfidRequestStatus status = RfidRequestStatus.PENDING;

    private Long issuedTagId;
}
