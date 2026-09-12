package com.cyrev.nitelestate.billing;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** A charge the CDA can levy on residents, e.g. Estate Levy, Security Levy (spec §4). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "levy")
public class Levy extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LevyFrequency frequency = LevyFrequency.ANNUAL;

    @Column(nullable = false)
    private boolean active = true;

    /** Marks this as *the* vehicle sticker fee, if any levy is - StickerRequestService resolves
     * the active levy flagged here to know which levy/invoice a sticker request should charge
     * against, without hardcoding a levy id or name. At most one should realistically be flagged
     * active at a time; nothing in the schema enforces that, it's an operational convention. */
    @Column(nullable = false)
    private boolean vehicleStickerLevy = false;
}
