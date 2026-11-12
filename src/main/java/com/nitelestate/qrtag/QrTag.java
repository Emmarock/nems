package com.nitelestate.qrtag;

import com.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** A pre-generated, printable QR tag - see V45__qr_tag.sql and QrTagService. */
@Getter
@Setter
@Entity
@Table(name = "qr_tag")
public class QrTag extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QrTagKind kind;

    @Column(nullable = false)
    private Integer serial;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private Integer batchNo;

    private Long subjectId;
    private Instant assignedAt;
    private Long assignedByUserId;
}
