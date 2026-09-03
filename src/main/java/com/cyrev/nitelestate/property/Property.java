package com.cyrev.nitelestate.property;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The estate's property registry per Nitel_Estate_Management_System_NEMS.md §2. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "property")
public class Property extends BaseEntity {

    @Column(nullable = false)
    private String block;

    @Column(nullable = false)
    private String plot;

    @Column(nullable = false, unique = true)
    private String houseNumber;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PropertyType propertyType;

    /** The owning resident, if registered. */
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OccupancyStatus occupancyStatus = OccupancyStatus.VACANT;

    /** Enforcement QR pass — scanned to pull up this building's payment history. */
    private String qrToken;
}
