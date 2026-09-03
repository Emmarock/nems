package com.cyrev.nitelestate.rfid;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A physical RFID credential for residents/long-term workers, ahead of full ANPR (spec Phase 3 §6). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rfid_tag")
public class RfidTag extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String tagId;

    private Long assignedResidentId;
    private Long assignedWorkerId;
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RfidStatus status = RfidStatus.ACTIVE;
}
