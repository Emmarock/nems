package com.nitelestate.qrtag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QrTagRepository extends JpaRepository<QrTag, Long> {

    Optional<QrTag> findByCodeIgnoreCase(String code);

    Optional<QrTag> findByToken(String token);

    List<QrTag> findByBatchNoOrderBySerial(Integer batchNo);

    /** The tag currently carrying this vehicle/property's token, if its pass came from a printed tag. */
    Optional<QrTag> findByKindAndToken(QrTagKind kind, String token);

    @Query("select coalesce(max(t.serial), 0) from QrTag t where t.kind = :kind")
    int maxSerial(QrTagKind kind);

    @Query("select coalesce(max(t.batchNo), 0) from QrTag t")
    int maxBatchNo();

    interface BatchRow {
        Integer getBatchNo();
        QrTagKind getKind();
        Long getTotal();
        Long getLinked();
        String getFirstCode();
        String getLastCode();
        Instant getCreatedAt();
    }

    @Query("""
            select t.batchNo as batchNo, t.kind as kind, count(t) as total, count(t.subjectId) as linked,
                   min(t.code) as firstCode, max(t.code) as lastCode, min(t.createdAt) as createdAt
            from QrTag t group by t.batchNo, t.kind order by t.batchNo desc
            """)
    List<BatchRow> batches();
}
