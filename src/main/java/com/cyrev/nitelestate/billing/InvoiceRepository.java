package com.cyrev.nitelestate.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(i.amount), 0) from Invoice i where i.residentId = :residentId and i.status = 'ISSUED'")
    BigDecimal sumIssuedAmountByResident(Long residentId);

    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(i.amount), 0) from Invoice i where i.status = 'ISSUED'")
    BigDecimal sumAllIssuedAmount();

    /**
     * Row shape: [levyId (Long), levyName (String), totalDue (BigDecimal)]. Left-joins the levy
     * catalog rather than requiring a match, so invoices under a since-deleted levy (e.g. a
     * retired category like the old "2024 Arrears" bucket) still show up here — falling back to
     * the invoice's own description snapshot for a name — instead of silently vanishing from the
     * breakdown while still counting toward the resident's overall balance.
     */
    @org.springframework.data.jpa.repository.Query(value = """
            select i.levy_id, coalesce(max(l.name), max(i.description)), coalesce(sum(i.amount), 0)
            from invoice i left join levy l on l.id = i.levy_id
            where i.resident_id = :residentId and i.status = 'ISSUED'
            group by i.levy_id
            """, nativeQuery = true)
    List<Object[]> sumIssuedAmountByResidentGroupedByLevy(Long residentId);
}
