package com.cyrev.nitelestate.worker;

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
 * The Worker Module — added to Phase 2 because Nitel Estate is still under development and
 * labourers/contractors come on site regularly, needing repeated multi-day access distinct
 * from a one-off Visitor pass (Nitel_Estate_Phase_2_Resident_Experience.md §4).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "worker")
public class Worker extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    private String nationalId;

    @Column(nullable = false)
    private String contractorName;

    @Column(nullable = false)
    private String workType;

    /** The property/site this worker is assigned to. */
    private Long siteId;

    /** The resident or estate developer who requested/sponsors this worker's access. */
    @Column(nullable = false)
    private Long sponsorResidentId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate expectedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WorkerStatus status = WorkerStatus.PENDING;

    @Column(unique = true)
    private String qrToken;

    /**
     * A photo of the worker, submitted at request time — lets security visually confirm
     * identity against the QR pass at the gate. Stored as a base64 data URI; the frontend
     * downscales/compresses before upload to keep this small (see WorkerService.request).
     */
    @Column(columnDefinition = "text")
    private String photo;
}
