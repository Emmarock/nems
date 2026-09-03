package com.cyrev.nitelestate.visitor;

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

/** Short-term visitor pass, created by a resident (spec §9). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "visitor")
public class Visitor extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String vehiclePlate;

    @Column(nullable = false)
    private Long hostResidentId;

    @Column(nullable = false)
    private Instant validFrom;

    @Column(nullable = false)
    private Instant validUntil;

    @Column(nullable = false, unique = true)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VisitorStatus status = VisitorStatus.ACTIVE;
}
