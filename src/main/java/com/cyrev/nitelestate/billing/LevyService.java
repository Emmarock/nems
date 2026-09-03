package com.cyrev.nitelestate.billing;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.billing.dto.LevyRequest;
import com.cyrev.nitelestate.billing.dto.LevyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LevyService {

    private final LevyRepository levyRepository;
    private final AuditService auditService;

    @Transactional
    public LevyResponse create(LevyRequest request) {
        Levy levy = new Levy();
        apply(levy, request);
        levy = levyRepository.save(levy);
        auditService.record("Levy", levy.getId(), "CREATE", levy.getName());
        return LevyResponse.from(levy);
    }

    @Transactional
    public LevyResponse update(Long id, LevyRequest request) {
        Levy levy = get(id);
        apply(levy, request);
        levy = levyRepository.save(levy);
        auditService.record("Levy", levy.getId(), "UPDATE", levy.getName());
        return LevyResponse.from(levy);
    }

    public PageResponse<LevyResponse> search(Pageable pageable) {
        return PageResponse.of(levyRepository.findAll(pageable), LevyResponse::from);
    }

    Levy get(Long id) {
        return levyRepository.findById(id).orElseThrow(() -> NotFoundException.of("Levy", id));
    }

    private void apply(Levy levy, LevyRequest request) {
        levy.setName(request.name());
        levy.setAmount(request.amount());
        levy.setFrequency(request.frequency());
        levy.setActive(request.active() == null || request.active());
    }
}
