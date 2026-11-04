package com.nitelestate.facematch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** One row = the one-time backfill has completed. */
@Getter
@Setter
@Entity
@Table(name = "face_index_backfill")
public class FaceIndexBackfill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int indexed;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private Instant completedAt;
}
