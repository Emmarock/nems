package com.nitelestate.visitor;

import com.nitelestate.common.BaseEntity;
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

    /** Scanning this one always checks the visitor in — see VisitorService.resolve. */
    @Column(nullable = false, unique = true)
    private String qrTokenEntry;

    /** Null until the visitor's first check-in — generated lazily then, so a resident can't hand
     * out an exit pass before there's anything to exit from, and a no-show never gets one at all.
     * Scanning it always checks the visitor out — see VisitorService.resolve/scan. */
    @Column(unique = true)
    private String qrTokenExit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VisitorStatus status = VisitorStatus.ACTIVE;

    /**
     * A photo of the visitor, submitted at pass-creation time — lets security visually confirm
     * identity against the QR pass at the gate. Stored as a base64 data URI; the frontend
     * downscales/compresses before upload to keep this small (see VisitorService.create).
     */
    @Column(columnDefinition = "text")
    private String photo;
}
