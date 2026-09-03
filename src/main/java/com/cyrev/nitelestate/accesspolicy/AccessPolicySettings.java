package com.cyrev.nitelestate.accesspolicy;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A single configurable settings row rather than a rule DSL/engine (spec Phase 3 §3 —
 * "AccessPolicy is a configurable settings object evaluated in code, not a rule engine").
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "access_policy_settings")
public class AccessPolicySettings extends BaseEntity {

    @Column(nullable = false)
    private boolean enforceArrears = true;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal arrearsThreshold = BigDecimal.ZERO;
}
