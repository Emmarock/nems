package com.cyrev.nitelestate.sticker;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.billing.Invoice;
import com.cyrev.nitelestate.billing.InvoiceRepository;
import com.cyrev.nitelestate.billing.InvoiceService;
import com.cyrev.nitelestate.billing.Levy;
import com.cyrev.nitelestate.billing.LevyRepository;
import com.cyrev.nitelestate.billing.dto.InvoiceResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.ConflictException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.sticker.dto.StickerRequestResponse;
import com.cyrev.nitelestate.vehicle.Vehicle;
import com.cyrev.nitelestate.vehicle.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Vehicle sticker requests. A sticker can only be requested for a vehicle the resident actually
 * owns, at most once per vehicle (see StickerRequest.vehicleId), and only becomes ISSUED once a
 * Payment against its linked invoice is approved - see onPaymentSucceeded, called from
 * PaymentService whenever a payment transitions to SUCCESS.
 */
@Service
@RequiredArgsConstructor
public class StickerRequestService {

    private final StickerRequestRepository stickerRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final LevyRepository levyRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final ResidentRepository residentRepository;
    private final AuditService auditService;

    @Transactional
    public StickerRequestResponse request(Long residentId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BadRequestException("No vehicle found with id " + vehicleId));
        if (!vehicle.getResidentId().equals(residentId)) {
            throw new BadRequestException("You can only request a sticker for your own vehicle");
        }
        if (stickerRequestRepository.existsByVehicleId(vehicleId)) {
            throw new ConflictException("A sticker has already been requested for this vehicle");
        }
        Levy levy = levyRepository.findFirstByVehicleStickerLevyTrueAndActiveTrue()
                .orElseThrow(() -> new BadRequestException(
                        "No active vehicle sticker levy is configured - contact the estate office"));

        InvoiceResponse invoice = invoiceService.findOrGenerate(residentId, levy.getId());

        StickerRequest sr = new StickerRequest();
        sr.setVehicleId(vehicleId);
        sr.setResidentId(residentId);
        sr.setInvoiceId(invoice.id());
        sr.setStatus(StickerStatus.PENDING_PAYMENT);
        sr = stickerRequestRepository.save(sr);

        auditService.record("StickerRequest", sr.getId(), "REQUEST",
                "vehicle=" + vehicle.getPlateNumber() + " invoice=" + invoice.id());
        return StickerRequestResponse.from(sr, vehicle.getPlateNumber(), resolveResidentName(residentId), invoice.amount());
    }

    /** Called by PaymentService once a payment transitions to SUCCESS - issues the sticker if
     * that payment's invoice happens to be one a sticker request is waiting on. A no-op otherwise
     * (most payments aren't for a sticker at all), and idempotent if called more than once. */
    @Transactional
    public void onPaymentSucceeded(Long invoiceId) {
        stickerRequestRepository.findByInvoiceId(invoiceId).ifPresent(sr -> {
            if (sr.getStatus() != StickerStatus.ISSUED) {
                sr.setStatus(StickerStatus.ISSUED);
                sr.setIssuedAt(Instant.now());
                stickerRequestRepository.save(sr);
                auditService.record("StickerRequest", sr.getId(), "ISSUE", null);
            }
        });
    }

    /** residentId null lists every sticker request (admin/treasurer view); non-null scopes to one
     * resident's own ("My vehicles" / sticker status). */
    public PageResponse<StickerRequestResponse> search(Long residentId, Pageable pageable) {
        Specification<StickerRequest> spec = Specification.<StickerRequest>where(Specs.eq(residentId, "residentId"));
        Page<StickerRequest> page = stickerRequestRepository.findAll(spec, pageable);

        Map<Long, Vehicle> vehicles = vehicleRepository.findAllById(
                page.getContent().stream().map(StickerRequest::getVehicleId).distinct().toList())
                .stream().collect(Collectors.toMap(Vehicle::getId, Function.identity()));
        Map<Long, String> residentNames = residentRepository.findAllById(
                page.getContent().stream().map(StickerRequest::getResidentId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Resident::getId, Resident::getFullName));
        Map<Long, BigDecimal> invoiceAmounts = invoiceRepository.findAllById(
                page.getContent().stream().map(StickerRequest::getInvoiceId).distinct().toList())
                .stream().collect(Collectors.toMap(Invoice::getId, Invoice::getAmount));

        return PageResponse.of(page.map(sr -> {
            Vehicle vehicle = vehicles.get(sr.getVehicleId());
            return StickerRequestResponse.from(sr, vehicle != null ? vehicle.getPlateNumber() : null,
                    residentNames.get(sr.getResidentId()), invoiceAmounts.get(sr.getInvoiceId()));
        }));
    }

    private String resolveResidentName(Long residentId) {
        return residentRepository.findById(residentId).map(Resident::getFullName).orElse(null);
    }
}
