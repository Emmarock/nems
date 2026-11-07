package com.nitelestate.user;

import com.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** The resident currently holding an estate office (one per office) - see OfficeHolderService. */
@Getter
@Setter
@Entity
@Table(name = "office_holder")
public class OfficeHolder extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private Role role;

    @Column(nullable = false)
    private Long residentId;

    private Long assignedByUserId;
}
