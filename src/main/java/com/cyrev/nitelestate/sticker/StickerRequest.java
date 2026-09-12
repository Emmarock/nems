package com.cyrev.nitelestate.sticker;

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
 * A resident's request for a vehicle window sticker - one row per vehicle (vehicle_id is unique,
 * see V18__sticker_request.sql), tied to the invoice that must be paid (and approved) before it
 * can be issued. requestedAt is BaseEntity.createdAt; no separate column needed.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sticker_request")
public class StickerRequest extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long vehicleId;

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false)
    private Long invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StickerStatus status = StickerStatus.PENDING_PAYMENT;

    private Instant issuedAt;
}
