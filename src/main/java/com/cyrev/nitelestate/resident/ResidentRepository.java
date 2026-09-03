package com.cyrev.nitelestate.resident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long>, JpaSpecificationExecutor<Resident> {
    Optional<Resident> findByQrToken(String qrToken);

    @Query("select r.id from Resident r")
    List<Long> findAllIds();

    /**
     * Counts residents whose (issued invoices - successful payments) exceeds the threshold, in
     * one query — replaces looping AccessPolicyService.evaluateResident() over every resident,
     * which was O(n) round-trips and became slow once the resident table held real data.
     */
    @Query(value = """
            select count(*) from (
                select r.id,
                    coalesce((select sum(i.amount) from invoice i where i.resident_id = r.id and i.status = 'ISSUED'), 0)
                    - coalesce((select sum(p.amount) from payment p where p.resident_id = r.id and p.status = 'SUCCESS'), 0) as outstanding
                from resident r
            ) balances
            where outstanding > :threshold
            """, nativeQuery = true)
    long countResidentsWithOutstandingAbove(BigDecimal threshold);

    /**
     * Same balance calculation as {@link #countResidentsWithOutstandingAbove}, but returns the
     * page of residents themselves (biggest balance first) — backs the "Accounts in arrears"
     * drill-through on the security dashboard, so the list a treasurer/security officer sees
     * always matches the count shown there exactly (same threshold, same formula).
     */
    @Query(value = """
            select id, outstanding from (
                select r.id as id, r.full_name as full_name, r.phone as phone,
                    coalesce((select sum(i.amount) from invoice i where i.resident_id = r.id and i.status = 'ISSUED'), 0)
                    - coalesce((select sum(p.amount) from payment p where p.resident_id = r.id and p.status = 'SUCCESS'), 0) as outstanding
                from resident r
            ) balances
            where outstanding > :threshold
              and (:q = '' or lower(full_name) like '%' || lower(:q) || '%' or lower(phone) like '%' || lower(:q) || '%')
            order by outstanding desc
            """,
            countQuery = """
            select count(*) from (
                select r.id, r.full_name as full_name, r.phone as phone,
                    coalesce((select sum(i.amount) from invoice i where i.resident_id = r.id and i.status = 'ISSUED'), 0)
                    - coalesce((select sum(p.amount) from payment p where p.resident_id = r.id and p.status = 'SUCCESS'), 0) as outstanding
                from resident r
            ) balances
            where outstanding > :threshold
              and (:q = '' or lower(full_name) like '%' || lower(:q) || '%' or lower(phone) like '%' || lower(:q) || '%')
            """,
            nativeQuery = true)
    Page<ResidentArrearsRow> findResidentsInArrears(BigDecimal threshold, String q, Pageable pageable);

    interface ResidentArrearsRow {
        Long getId();
        BigDecimal getOutstanding();
    }
}
