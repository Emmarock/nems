package com.cyrev.nitelestate.billing;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.billing.dto.InvoiceGenerateRequest;
import com.cyrev.nitelestate.billing.dto.InvoiceResponse;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final LevyRepository levyRepository;
    private final ResidentRepository residentRepository;
    private final AuditService auditService;

    @Transactional
    public InvoiceResponse generate(InvoiceGenerateRequest request) {
        Levy levy = levyRepository.findById(request.levyId())
                .orElseThrow(() -> NotFoundException.of("Levy", request.levyId()));

        Invoice invoice = new Invoice();
        invoice.setResidentId(request.residentId());
        invoice.setLevyId(levy.getId());
        invoice.setDescription(levy.getName());
        invoice.setAmount(levy.getAmount());
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(request.dueDate() != null ? request.dueDate() : LocalDate.now().plusMonths(1));
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice = invoiceRepository.save(invoice);

        auditService.record("Invoice", invoice.getId(), "GENERATE",
                "resident=" + invoice.getResidentId() + " levy=" + levy.getName());
        return InvoiceResponse.from(invoice, resolveResidentName(invoice.getResidentId()));
    }

    @Transactional
    public InvoiceResponse cancel(Long id) {
        Invoice invoice = get(id);
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice = invoiceRepository.save(invoice);
        auditService.record("Invoice", invoice.getId(), "CANCEL", null);
        return InvoiceResponse.from(invoice, resolveResidentName(invoice.getResidentId()));
    }

    public PageResponse<InvoiceResponse> search(String q, Long residentId, Pageable pageable) {
        Specification<Invoice> spec = Specification.<Invoice>where(Specs.contains(q, "description"))
                .or(residentNameContains(q))
                .and(Specs.eq(residentId, "residentId"));
        var page = invoiceRepository.findAll(spec, pageable);

        List<Long> residentIds = page.getContent().stream().map(Invoice::getResidentId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> residentNames = residentRepository.findAllById(residentIds).stream()
                .collect(Collectors.toMap(Resident::getId, Resident::getFullName));

        return PageResponse.of(page.map(i -> InvoiceResponse.from(i, residentNames.get(i.getResidentId()))));
    }

    /** Invoice has no JPA relation to Resident (plain FK long), so matching by owner name needs a subquery. */
    private Specification<Invoice> residentNameContains(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            var residentRoot = sub.from(Resident.class);
            sub.select(residentRoot.get("id")).where(cb.like(cb.lower(residentRoot.get("fullName")), pattern));
            return root.get("residentId").in(sub);
        };
    }

    private String resolveResidentName(Long residentId) {
        return residentId == null ? null : residentRepository.findById(residentId).map(Resident::getFullName).orElse(null);
    }

    private Invoice get(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> NotFoundException.of("Invoice", id));
    }
}
