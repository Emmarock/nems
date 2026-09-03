package com.cyrev.nitelestate.resident;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A resident's profile per Nitel_Estate_Management_System_NEMS.md §1.
 * `propertyId` is the property they occupy; landlord/tenant relationships are modelled by
 * Property.ownerId (the owning resident) plus each occupant Resident's residentType.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resident")
public class Resident extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    private String email;

    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ResidentType residentType;

    private String emergencyContact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ResidentStatus status = ResidentStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDate registrationDate = LocalDate.now();

    /** A resident's own gate-access QR pass — generated lazily on first request, see ResidentService. */
    @Column(unique = true)
    private String qrToken;
}
