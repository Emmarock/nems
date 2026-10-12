package com.cyrev.nitelestate.payment;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.billing.Invoice;
import com.cyrev.nitelestate.billing.InvoiceRepository;
import com.cyrev.nitelestate.billing.Levy;
import com.cyrev.nitelestate.billing.LevyRepository;
import com.cyrev.nitelestate.billing.dto.InvoiceResponse;
import com.cyrev.nitelestate.billing.InvoiceService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.notification.MessageTemplates;
import com.cyrev.nitelestate.notification.NotificationAttachment;
import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationService;
import com.cyrev.nitelestate.payment.dto.*;
import com.cyrev.nitelestate.property.Property;
import com.cyrev.nitelestate.property.PropertyRepository;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.resident.ResidentType;
import com.cyrev.nitelestate.sticker.StickerRequestService;
import com.cyrev.nitelestate.user.User;
import com.cyrev.nitelestate.user.UserRepository;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final SharedReceiptRepository sharedReceiptRepository;
    private final ResidentRepository residentRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final LevyRepository levyRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final StickerRequestService stickerRequestService;
    private final NotificationService notificationService;
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
        sendReceipt(payment);
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
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> NotFoundException.of("Resident", residentId));
        if (resident.getResidentType() == ResidentType.TENANT) {
            throw new BadRequestException(
                    "Tenants can't submit a payment receipt directly — share it with your landlord from the Payments page; they'll submit it.");
        }
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

    /**
     * What a tenant does instead of submitReceipt: hands the receipt to their landlord (resolved
     * server-side, never trusted from the request) rather than minting a payment under their own
     * (unbilled) resident id. Notifies the landlord on every channel they have a real contact
     * method for, same convention as every other notification in this codebase.
     */
    @Transactional
    public SharedReceiptResponse shareReceiptWithLandlord(Long tenantResidentId, PaymentReceiptRequest request) {
        if (request.receiptImage() != null && request.receiptImage().length() > MAX_RECEIPT_LENGTH) {
            throw new BadRequestException("Receipt image is too large — please use a smaller image");
        }
        Levy levy = levyRepository.findById(request.levyId())
                .orElseThrow(() -> NotFoundException.of("Levy", request.levyId()));
        Long landlordResidentId = resolveLandlordResidentId(tenantResidentId);

        SharedReceipt shared = new SharedReceipt();
        shared.setTenantResidentId(tenantResidentId);
        shared.setLandlordResidentId(landlordResidentId);
        shared.setLevyId(levy.getId());
        shared.setAmount(request.amount());
        shared.setMethod(request.method());
        shared.setReceiptImage(request.receiptImage());
        shared = sharedReceiptRepository.save(shared);

        auditService.record("SharedReceipt", shared.getId(), "SHARE",
                "tenant=" + tenantResidentId + " landlord=" + landlordResidentId + " levy=" + levy.getName());
        notifyLandlordOfSharedReceipt(tenantResidentId, landlordResidentId);
        return SharedReceiptResponse.from(shared, resolveResidentName(tenantResidentId), levy.getName());
    }

    /** Pending shares awaiting the landlord's review — see submitSharedReceipt/dismissSharedReceipt. */
    public PageResponse<SharedReceiptResponse> listSharedReceiptsForLandlord(Long landlordResidentId, Pageable pageable) {
        Page<SharedReceipt> page = sharedReceiptRepository.findByLandlordResidentIdAndStatus(
                landlordResidentId, SharedReceiptStatus.PENDING, pageable);
        return PageResponse.of(page.map(r -> SharedReceiptResponse.from(r, resolveResidentName(r.getTenantResidentId()),
                levyRepository.findById(r.getLevyId()).map(Levy::getName).orElse(null))));
    }

    /**
     * The landlord reviewed a shared receipt and confirmed it's legitimate — submits it exactly
     * as if they'd filled in the form themselves (same method, same resulting PENDING_APPROVAL
     * Payment under their own account), then marks the share consumed.
     */
    @Transactional
    public PaymentResponse submitSharedReceipt(Long landlordResidentId, Long sharedReceiptId) {
        SharedReceipt shared = getSharedReceipt(sharedReceiptId);
        requireOwnedShare(shared, landlordResidentId);

        PaymentResponse payment = submitReceipt(landlordResidentId,
                new PaymentReceiptRequest(shared.getLevyId(), shared.getAmount(), shared.getMethod(), shared.getReceiptImage()));

        shared.setStatus(SharedReceiptStatus.SUBMITTED);
        sharedReceiptRepository.save(shared);
        auditService.record("SharedReceipt", shared.getId(), "SUBMIT", "payment=" + payment.id());
        return payment;
    }

    /** The landlord decided a shared receipt doesn't need submitting (e.g. a duplicate) — clears it from their queue. */
    @Transactional
    public void dismissSharedReceipt(Long landlordResidentId, Long sharedReceiptId) {
        SharedReceipt shared = getSharedReceipt(sharedReceiptId);
        requireOwnedShare(shared, landlordResidentId);
        shared.setStatus(SharedReceiptStatus.DISMISSED);
        sharedReceiptRepository.save(shared);
        auditService.record("SharedReceipt", shared.getId(), "DISMISS", null);
    }

    private void requireOwnedShare(SharedReceipt shared, Long landlordResidentId) {
        if (!shared.getLandlordResidentId().equals(landlordResidentId)) {
            throw new BadRequestException("That shared receipt isn't yours to act on");
        }
        if (shared.getStatus() != SharedReceiptStatus.PENDING) {
            throw new BadRequestException("That shared receipt has already been handled");
        }
    }

    private SharedReceipt getSharedReceipt(Long id) {
        return sharedReceiptRepository.findById(id).orElseThrow(() -> NotFoundException.of("SharedReceipt", id));
    }

    /**
     * Duplicated from ResidentService.resolveLandlordResidentId (same logic, same two
     * exceptions) rather than injecting ResidentService here — that would close a dependency
     * cycle: AccessPolicyService -> AccountService -> PaymentService -> ResidentService ->
     * AccessPolicyService. PropertyRepository alone carries no such risk.
     */
    private Long resolveLandlordResidentId(Long residentId) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> NotFoundException.of("Resident", residentId));
        if (resident.getPropertyId() == null) {
            throw new BadRequestException("You don't have a property on file yet — contact an admin to get one linked");
        }
        Property property = propertyRepository.findById(resident.getPropertyId())
                .orElseThrow(() -> NotFoundException.of("Property", resident.getPropertyId()));
        if (property.getOwnerId() == null) {
            throw new BadRequestException("Your property doesn't have a landlord on file yet — contact an admin");
        }
        return property.getOwnerId();
    }

    private void notifyLandlordOfSharedReceipt(Long tenantResidentId, Long landlordResidentId) {
        Resident tenant = residentRepository.findById(tenantResidentId).orElse(null);
        Resident landlord = residentRepository.findById(landlordResidentId).orElse(null);
        if (tenant == null || landlord == null) {
            return;
        }
        String message = MessageTemplates.receiptShared(tenant.getFullName());
        if (landlord.getEmail() != null) {
            notificationService.dispatch(NotificationChannel.EMAIL, landlord.getEmail(), message);
        }
        if (landlord.getPhone() != null) {
            notificationService.dispatch(NotificationChannel.SMS, landlord.getPhone(), message);
            notificationService.dispatch(NotificationChannel.WHATSAPP, landlord.getPhone(), message);
        }
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
        sendReceipt(payment);
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

    /**
     * Emails the paying resident a receipt (PDF attached) the moment staff confirm their payment
     * - whether via approve() (a submitted receipt matched against the bank alert) or
     * recordManual() (staff entering an offline payment directly, which is itself the approval
     * act). Silently skipped if the resident has no email on file - there's no one to send it to,
     * not a failure worth surfacing to the approving staff member.
     */
    private void sendReceipt(Payment payment) {
        Resident resident = residentRepository.findById(payment.getResidentId()).orElse(null);
        if (resident == null || resident.getEmail() == null || resident.getEmail().isBlank()) {
            return;
        }
        String levyDescription = payment.getInvoiceId() != null
                ? invoiceRepository.findById(payment.getInvoiceId()).map(Invoice::getDescription).orElse(null)
                : null;
        Property property = resident.getPropertyId() != null
                ? propertyRepository.findById(resident.getPropertyId()).orElse(null)
                : null;
        String houseNumber = property != null ? property.getHouseNumber() : null;
        String fullAddress = composeReceiptAddress(property);
        String receiptNumber = "NECDA-PYMT-RCPT-"+System.currentTimeMillis() +"-"+ payment.getId();
        String message = MessageTemplates.paymentReceipt(resident.getFullName(), receiptNumber, levyDescription,
                payment.getAmount(), payment.getPaidAt());
        byte[] pdf = PaymentReceiptPdf.render(resident.getFullName(), receiptNumber, levyDescription,
                payment.getAmount(), payment.getMethod().name(), payment.getPaidAt(), houseNumber, fullAddress);
        NotificationAttachment attachment = new NotificationAttachment(receiptNumber + ".pdf", "application/pdf", pdf);
        notificationService.dispatch(NotificationChannel.EMAIL, resident.getEmail(), message,
                "Payment receipt " + receiptNumber, attachment);
    }

    /**
     * The receipt's address line is composed fresh from house number + road + postcode + the
     * estate's fixed location, rather than reusing Property.address (free text, inconsistently
     * filled in across the 2025/2026 data imports) - this way every receipt reads the same way
     * regardless of what ended up in that field. postcode is left out when not on file (Nigeria's
     * federal postcode rollout is recent); no property at all means no address line.
     */
    private String composeReceiptAddress(Property property) {
        if (property == null) {
            return null;
        }
        StringBuilder address = new StringBuilder()
                .append(property.getHouseNumber()).append(", ").append(property.getRoad()).append(", ");
        if (property.getPostcode() != null && !property.getPostcode().isBlank()) {
            address.append(property.getPostcode()).append(", ");
        }
        return address.append("Nitel Estate Ikorodu Lagos").toString();
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
