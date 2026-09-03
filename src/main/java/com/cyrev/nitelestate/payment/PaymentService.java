package com.cyrev.nitelestate.payment;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.payment.dto.*;
import com.cyrev.nitelestate.payment.provider.PaymentInitiationResult;
import com.cyrev.nitelestate.payment.provider.PaymentProvider;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;
    private final ResidentRepository residentRepository;
    private final AuditService auditService;

    @Transactional
    public PaymentResponse recordManual(PaymentRecordRequest request, Long recordedByUserId) {
        Payment payment = new Payment();
        payment.setResidentId(request.residentId());
        payment.setInvoiceId(request.invoiceId());
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        payment.setRecordedByUserId(recordedByUserId);
        payment = paymentRepository.save(payment);
        auditService.record("Payment", payment.getId(), "RECORD_MANUAL",
                "resident=" + payment.getResidentId() + " amount=" + payment.getAmount());
        return PaymentResponse.from(payment, resolveResidentName(payment.getResidentId()));
    }

    @Transactional
    public OnlinePaymentInitiateResponse initiateOnline(Long residentId, OnlinePaymentInitiateRequest request) {
        Payment payment = new Payment();
        payment.setResidentId(residentId);
        payment.setInvoiceId(request.invoiceId());
        payment.setAmount(request.amount());
        payment.setMethod(PaymentMethod.ONLINE_GATEWAY);
        payment.setProvider(paymentProvider.key());
        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        PaymentInitiationResult result = paymentProvider.initiate(
                residentId, request.invoiceId(), request.amount(), "PAYMENT-" + payment.getId());
        payment.setProviderReference(result.providerReference());
        payment = paymentRepository.save(payment);

        auditService.record("Payment", payment.getId(), "INITIATE_ONLINE",
                "resident=" + residentId + " amount=" + request.amount());
        return new OnlinePaymentInitiateResponse(payment.getId(), result.providerReference(), result.redirectUrl());
    }

    @Transactional
    public PaymentResponse handleWebhook(PaymentWebhookPayload payload) {
        Payment payment = paymentRepository.findByProviderReference(payload.providerReference())
                .orElseThrow(() -> new NotFoundException("No payment found for reference " + payload.providerReference()));

        PaymentStatus newStatus = switch (payload.status().toUpperCase()) {
            case "SUCCESS", "SUCCESSFUL" -> PaymentStatus.SUCCESS;
            case "FAILED" -> PaymentStatus.FAILED;
            default -> throw new BadRequestException("Unrecognised webhook status: " + payload.status());
        };
        payment.setStatus(newStatus);
        payment.setPaidAt(Instant.now());
        payment = paymentRepository.save(payment);

        auditService.record("Payment", payment.getId(), "WEBHOOK_" + newStatus, payload.providerReference());
        return PaymentResponse.from(payment, resolveResidentName(payment.getResidentId()));
    }

    public PaymentResponse findById(Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Payment", id));
        return PaymentResponse.from(payment, resolveResidentName(payment.getResidentId()));
    }

    public PageResponse<PaymentResponse> search(String q, Long residentId, Pageable pageable) {
        Specification<Payment> spec = Specification.<Payment>where(Specs.contains(q, "providerReference"))
                .or(residentNameContains(q))
                .and(Specs.eq(residentId, "residentId"));
        var page = paymentRepository.findAll(spec, pageable);

        List<Long> residentIds = page.getContent().stream().map(Payment::getResidentId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> residentNames = residentRepository.findAllById(residentIds).stream()
                .collect(Collectors.toMap(Resident::getId, Resident::getFullName));

        return PageResponse.of(page.map(p -> PaymentResponse.from(p, residentNames.get(p.getResidentId()))));
    }

    /** Payment has no JPA relation to Resident (plain FK long), so matching by owner name needs a subquery. */
    private Specification<Payment> residentNameContains(String q) {
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
}
