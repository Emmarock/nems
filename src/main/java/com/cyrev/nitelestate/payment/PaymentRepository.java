package com.cyrev.nitelestate.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByProviderReference(String providerReference);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.residentId = :residentId and p.status = 'SUCCESS'")
    BigDecimal sumSuccessfulAmountByResident(Long residentId);

    /**
     * Row shape: [levyId (Long), totalPaid (BigDecimal)] — paid amounts attributed via each
     * payment's invoice. Left-joins the invoice rather than requiring a match: invoices are
     * never deleted today, but a payment whose invoice is somehow missing should still count
     * toward the resident's total rather than silently vanishing (matches the defensive style
     * used for the levy left-join in InvoiceRepository).
     */
    @Query(value = """
            select i.levy_id, coalesce(sum(p.amount), 0)
            from payment p left join invoice i on i.id = p.invoice_id
            where p.resident_id = :residentId and p.status = 'SUCCESS'
            group by i.levy_id
            """, nativeQuery = true)
    List<Object[]> sumSuccessfulAmountByResidentGroupedByLevy(Long residentId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = 'SUCCESS' and p.paidAt >= :from")
    BigDecimal sumSuccessfulAmountSince(java.time.Instant from);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = 'SUCCESS'")
    BigDecimal sumAllSuccessfulAmount();
}
