package com.cyrev.nitelestate.payment;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.billing.Levy;
import com.cyrev.nitelestate.billing.LevyRepository;
import com.cyrev.nitelestate.billing.dto.InvoiceResponse;
import com.cyrev.nitelestate.billing.InvoiceService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.payment.dto.*;
import com.cyrev.nitelestate.payment.provider.PaymentInitiationResult;
import com.cyrev.nitelestate.payment.provider.PaymentProvider;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.sticker.StickerRequestService;
import com.cyrev.nitelestate.user.User;
import com.cyrev.nitelestate.user.UserRepository;
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

    /** ~1.5MB of base64 - comfortably covers a compressed photo (the frontend downscales before upload). */
    private static final int MAX_RECEIPT_LENGTH = 2_000_000;

    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;
    private final ResidentRepository residentRepository;
    private final UserRepository userRepository;
    private final LevyRepository levyRepository;
    private final InvoiceService invoiceService;
    private final StickerRequestService stickerRequestService;
    private final AuditService auditService;

    /**
     * Back-office entry (spec §5 Phase 1) - a staff member confirming a bank transfer/cash/cheque
     * they've already verified. approvedByUserId is set to the same person: recording it manually
     * IS the approval act, there's no separate review step for this path.
     */
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
        payment.setApprovedByUserId(recordedByUserId);
        payment.setApprovedAt(Instant.now());
        payment = paymentRepository.save(payment);
        auditService.record("Payment", payment.getId(), "RECORD_MANUAL",
                "resident=" + payment.getResidentId() + " amount=" + payment.getAmount());
        notifyStickerOfSuccess(payment);
        return toResponse(payment);
    }

    /**
     * Resident self-service: "I paid this levy offline, here's my receipt" (spec: residents
     * submit a payment receipt for any levy; treasurer/financial secretary approve after
     * confirming the bank alert). Reuses (or creates) the resident's open invoice for this levy
     * so a resubmission after rejection attaches to the same invoice rather than a new one.
     */
    @Transactional
    public PaymentResponse submitReceipt(Long residentId, PaymentReceiptRequest request) {
        Levy levy = levyRepository.findById(request.levyId())
                .orElseThrow(() -> NotFoundException.of("Levy", request.levyId()));
        if (!levy.isActive()) {
            throw new BadRequestException("This levy is no longer active");
        }
        if (request.receiptImage() != null && request.receiptImage().length() > MAX_RECEIPT_LENGTH) {
            throw new BadRequestException("Receipt image is too large — please use a smaller image");
        }
        InvoiceResponse invoice = invoiceService.findOrGenerate(residentId, levy.getId());

        Payment payment = new Payment();
        payment.setResidentId(residentId);
        payment.setInvoiceId(invoice.id());
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setReceiptImage(request.receiptImage());
        payment.setStatus(PaymentStatus.PENDING_APPROVAL);
        payment.setPaidAt(Instant.now());
        payment = paymentRepository.save(payment);

        auditService.record("Payment", payment.getId(), "SUBMIT_RECEIPT",
                "resident=" + residentId + " levy=" + levy.getName() + " amount=" + request.amount());
        return toResponse(payment);
    }

    /** Treasurer/financial secretary confirms the bank alert matches - the payment counts as paid. */
    @Transactional
    public PaymentResponse approve(Long id, Long approverUserId, PaymentReviewRequest request) {
        Payment payment = get(id);
        if (payment.getStatus() != PaymentStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only payments pending approval can be approved");
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setApprovedByUserId(approverUserId);
        payment.setApprovedAt(Instant.now());
        payment.setReviewNotes(request.notes());
        payment = paymentRepository.save(payment);
        auditService.record("Payment", payment.getId(), "APPROVE", request.notes());
        notifyStickerOfSuccess(payment);
        return toResponse(payment);
    }

    /** Treasurer/financial secretary rejects a submitted receipt (e.g. no matching bank alert) -
     * the resident can submit a fresh receipt against the same invoice. */
    @Transactional
    public PaymentResponse reject(Long id, Long approverUserId, PaymentReviewRequest request) {
        Payment payment = get(id);
        if (payment.getStatus() != PaymentStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only payments pending approval can be rejected");
        }
        payment.setStatus(PaymentStatus.REJECTED);
        payment.setApprovedByUserId(approverUserId);
        payment.setApprovedAt(Instant.now());
        payment.setReviewNotes(request.notes());
        payment = paymentRepository.save(payment);
        auditService.record("Payment", payment.getId(), "REJECT", request.notes());
        return toResponse(payment);
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
        if (newStatus == PaymentStatus.SUCCESS) {
            notifyStickerOfSuccess(payment);
        }
        return toResponse(payment);
    }

    public PaymentResponse findById(Long id) {
        return toResponse(get(id));
    }

    public PageResponse<PaymentResponse> search(String q, Long residentId, PaymentStatus status, Pageable pageable) {
        Specification<Payment> spec = Specification.<Payment>where(Specs.contains(q, "providerReference"))
                .or(residentNameContains(q))
                .and(Specs.eq(residentId, "residentId"))
                .and(Specs.eq(status, "status"));
        var page = paymentRepository.findAll(spec, pageable);

        List<Long> residentIds = page.getContent().stream().map(Payment::getResidentId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> residentNames = residentRepository.findAllById(residentIds).stream()
                .collect(Collectors.toMap(Resident::getId, Resident::getFullName));

        List<Long> approverIds = page.getContent().stream().map(Payment::getApprovedByUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> approverNames = userRepository.findAllById(approverIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));

        return PageResponse.of(page.map(p -> PaymentResponse.from(p, residentNames.get(p.getResidentId()),
                approverNames.get(p.getApprovedByUserId()))));
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

    /** invoiceId is nullable on Payment (a general/unapplied payment) - stickers are always tied
     * to a specific invoice, so there's nothing to notify when one isn't set. */
    private void notifyStickerOfSuccess(Payment payment) {
        if (payment.getInvoiceId() != null) {
            stickerRequestService.onPaymentSucceeded(payment.getInvoiceId());
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        String residentName = resolveResidentName(payment.getResidentId());
        String approverName = payment.getApprovedByUserId() == null ? null
                : userRepository.findById(payment.getApprovedByUserId()).map(User::getFullName).orElse(null);
        return PaymentResponse.from(payment, residentName, approverName);
    }

    private String resolveResidentName(Long residentId) {
        return residentId == null ? null : residentRepository.findById(residentId).map(Resident::getFullName).orElse(null);
    }

    private Payment get(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Payment", id));
    }
}
