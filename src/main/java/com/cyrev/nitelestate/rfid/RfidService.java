package com.cyrev.nitelestate.rfid;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.ConflictException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.rfid.dto.RfidTagRequest;
import com.cyrev.nitelestate.rfid.dto.RfidTagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RfidService {

    private final RfidTagRepository rfidTagRepository;
    private final AuditService auditService;

    @Transactional
    public RfidTagResponse issue(RfidTagRequest request) {
        if (rfidTagRepository.existsByTagId(request.tagId())) {
            throw new ConflictException("RFID tag " + request.tagId() + " is already registered");
        }
        RfidTag tag = new RfidTag();
        tag.setTagId(request.tagId());
        tag.setAssignedResidentId(request.assignedResidentId());
        tag.setAssignedWorkerId(request.assignedWorkerId());
        tag.setVehicleId(request.vehicleId());
        tag.setStatus(RfidStatus.ACTIVE);
        tag = rfidTagRepository.save(tag);

        auditService.record("RfidTag", tag.getId(), "ISSUE", tag.getTagId());
        return RfidTagResponse.from(tag);
    }

    @Transactional
    public RfidTagResponse setStatus(Long id, RfidStatus status) {
        RfidTag tag = get(id);
        tag.setStatus(status);
        tag = rfidTagRepository.save(tag);
        auditService.record("RfidTag", tag.getId(), "STATUS_" + status, null);
        return RfidTagResponse.from(tag);
    }

    /** Gate-side verification by tag id; only ACTIVE tags grant access. */
    public RfidTagResponse verify(String tagId) {
        RfidTag tag = rfidTagRepository.findByTagId(tagId)
                .orElseThrow(() -> new NotFoundException("No RFID tag found: " + tagId));
        if (tag.getStatus() != RfidStatus.ACTIVE) {
            throw new BadRequestException("RFID tag " + tagId + " is " + tag.getStatus());
        }
        return RfidTagResponse.from(tag);
    }

    public PageResponse<RfidTagResponse> search(Pageable pageable) {
        return PageResponse.of(rfidTagRepository.findAll(pageable), RfidTagResponse::from);
    }

    private RfidTag get(Long id) {
        return rfidTagRepository.findById(id).orElseThrow(() -> NotFoundException.of("RfidTag", id));
    }
}
