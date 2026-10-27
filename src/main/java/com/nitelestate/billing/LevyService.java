package com.nitelestate.billing;

import com.nitelestate.audit.AuditService;
import com.nitelestate.billing.dto.LevyApplyResponse;
import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.billing.dto.LevyRequest;
import com.nitelestate.billing.dto.LevyResponse;
import com.nitelestate.common.search.Specs;
import com.nitelestate.paymentaccount.PaymentAccount;
import com.nitelestate.paymentaccount.PaymentAccountRepository;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.resident.ResidentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LevyService {

    private final LevyRepository levyRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final ResidentRepository residentRepository;
    private final InvoiceService invoiceService;
    private final AuditService auditService;

    @Transactional
    public LevyResponse create(LevyRequest request) {
        Levy levy = new Levy();
        apply(levy, request);
        levy = levyRepository.save(levy);
        auditService.record("Levy", levy.getId(), "CREATE", levy.getName());
        return LevyResponse.from(levy, resolvePaymentAccountLabel(levy.getPaymentAccountId()));
    }

    @Transactional
    public LevyResponse update(Long id, LevyRequest request) {
        Levy levy = get(id);
        apply(levy, request);
        levy = levyRepository.save(levy);
        auditService.record("Levy", levy.getId(), "UPDATE", levy.getName());
        return LevyResponse.from(levy, resolvePaymentAccountLabel(levy.getPaymentAccountId()));
    }

    /**
     * One-time catch-up: ensures every currently ACTIVE resident has an invoice for this levy, for
     * when a new levy should apply to the whole estate rather than just residents created from now
     * on. Reuses a resident's existing open invoice for this levy if one exists (same idempotency
     * as findOrGenerate), so clicking this more than once doesn't inflate anyone's amount due.
     */
    @Transactional
    public LevyApplyResponse applyToAllResidents(Long id) {
        Levy levy = get(id);
        if (!levy.isActive()) {
            throw new BadRequestException("Only an active levy can be applied to residents");
        }

        List<Resident> residents = residentRepository.findAll(
                Specification.<Resident>where(Specs.eq(ResidentStatus.ACTIVE, "status")));
        for (Resident resident : residents) {
            invoiceService.findOrGenerate(resident.getId(), levy.getId());
        }

        auditService.record("Levy", levy.getId(), "APPLY_TO_ALL_RESIDENTS", residents.size() + " resident(s)");
        return new LevyApplyResponse(residents.size());
    }

    public PageResponse<LevyResponse> search(Pageable pageable) {
        var page = levyRepository.findAll(pageable);

        List<Long> accountIds = page.getContent().stream()
                .map(Levy::getPaymentAccountId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> accountLabels = paymentAccountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(PaymentAccount::getId, PaymentAccount::getLabel));

        return PageResponse.of(page.map(l -> LevyResponse.from(l, accountLabels.get(l.getPaymentAccountId()))));
    }

    Levy get(Long id) {
        return levyRepository.findById(id).orElseThrow(() -> NotFoundException.of("Levy", id));
    }

    private void apply(Levy levy, LevyRequest request) {
        levy.setName(request.name());
        levy.setAmount(request.amount());
        levy.setFrequency(request.frequency());
        levy.setActive(request.active() == null || request.active());
        levy.setVehicleStickerLevy(request.vehicleStickerLevy() != null && request.vehicleStickerLevy());
        levy.setDefaultForNewResident(request.defaultForNewResident() != null && request.defaultForNewResident());
        if (request.paymentAccountId() != null && !paymentAccountRepository.existsById(request.paymentAccountId())) {
            throw new BadRequestException("No payment account found with id " + request.paymentAccountId());
        }
        levy.setPaymentAccountId(request.paymentAccountId());
    }

    private String resolvePaymentAccountLabel(Long paymentAccountId) {
        return paymentAccountId == null ? null
                : paymentAccountRepository.findById(paymentAccountId).map(PaymentAccount::getLabel).orElse(null);
    }
}
