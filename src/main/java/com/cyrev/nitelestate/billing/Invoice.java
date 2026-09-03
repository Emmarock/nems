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
import java.time.LocalDate;

/** A charge raised against a resident's account for a given levy (spec §4). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice")
public class Invoice extends BaseEntity {

    @Column(nullable = false)
    private Long residentId;

    @Column(nullable = false)
    private Long levyId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate issueDate = LocalDate.now();

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus status = InvoiceStatus.ISSUED;
}
