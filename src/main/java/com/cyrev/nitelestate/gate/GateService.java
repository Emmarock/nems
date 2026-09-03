package com.cyrev.nitelestate.gate;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.ConflictException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.gate.dto.GateRequest;
import com.cyrev.nitelestate.gate.dto.GateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GateService {

    private final GateRepository gateRepository;
    private final AuditService auditService;

    @Transactional
    public GateResponse create(GateRequest request) {
        if (gateRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ConflictException("A gate with code " + request.code() + " already exists");
        }
        Gate gate = new Gate();
        apply(gate, request);
        gate = gateRepository.save(gate);
        auditService.record("Gate", gate.getId(), "CREATE", gate.getCode());
        return GateResponse.from(gate);
    }

    @Transactional
    public GateResponse update(Long id, GateRequest request) {
        Gate gate = get(id);
        apply(gate, request);
        gate = gateRepository.save(gate);
        auditService.record("Gate", gate.getId(), "UPDATE", gate.getCode());
        return GateResponse.from(gate);
    }

    public PageResponse<GateResponse> search(Pageable pageable) {
        return PageResponse.of(gateRepository.findAll(pageable), GateResponse::from);
    }

    public GateResponse findById(Long id) {
        return GateResponse.from(get(id));
    }

    private Gate get(Long id) {
        return gateRepository.findById(id).orElseThrow(() -> NotFoundException.of("Gate", id));
    }

    private void apply(Gate gate, GateRequest request) {
        gate.setName(request.name());
        gate.setCode(request.code());
        gate.setLocation(request.location());
        gate.setType(request.type());
        if (request.status() != null) {
            gate.setStatus(request.status());
        }
    }
}
