package com.cyrev.nitelestate.vehicle;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vehicle registry, collected ahead of ANPR (Phase 4) per
 * Nitel_Estate_Management_System_NEMS.md §3 — "Later, ANPR simply consumes this existing data."
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle")
public class Vehicle extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String plateNumber;

    private String vehicleType;
    private String make;
    private String model;
    private String colour;

    @Column(nullable = false)
    private Long residentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VehicleStatus status = VehicleStatus.ACTIVE;

    /** The owning resident's own QR pass for this vehicle — generated lazily on first request. */
    @Column(unique = true)
    private String qrToken;
}
