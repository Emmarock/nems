package com.cyrev.nitelestate.gate;

import com.cyrev.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A physical gate the estate can reference in access decisions and logs (spec Phase 3 §2). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gate")
public class Gate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GateType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GateStatus status = GateStatus.ACTIVE;
}
