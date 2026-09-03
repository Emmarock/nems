package com.cyrev.nitelestate.complaint;

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

/** A resident-reported service request (spec §7). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "complaint")
public class Complaint extends BaseEntity {

    @Column(nullable = false)
    private Long residentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ComplaintCategory category;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    private String assignedTo;

    private Instant resolvedAt;
}
