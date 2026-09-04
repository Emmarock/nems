package com.cyrev.nitelestate.rfid;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.ConflictException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.rfid.dto.RfidTagRequest;
import com.cyrev.nitelestate.rfid.dto.RfidTagResponse;
import com.cyrev.nitelestate.vehicle.Vehicle;
import com.cyrev.nitelestate.vehicle.VehicleRepository;
import com.cyrev.nitelestate.worker.Worker;
import com.cyrev.nitelestate.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RfidService {

    private final RfidTagRepository rfidTagRepository;
    private final ResidentRepository residentRepository;
    private final WorkerRepository workerRepository;
    private final VehicleRepository vehicleRepository;
    private final AuditService auditService;

    /**
     * A worker-assigned tag must be tied to the resident who sponsors that worker - the same
     * "connected to a resident" relationship a Worker already has (WorkerService.request), just
     * carried onto the tag too. If the caller only sent assignedWorkerId, the resident is
     * resolved from the worker automatically; if both were sent, they must agree.
     */
    @Transactional
    public RfidTagResponse issue(RfidTagRequest request) {
        if (rfidTagRepository.existsByTagId(request.tagId())) {
            throw new ConflictException("RFID tag " + request.tagId() + " is already registered");
        }

        Long residentId = request.assignedResidentId();
        if (request.assignedWorkerId() != null) {
            Worker worker = workerRepository.findById(request.assignedWorkerId())
                    .orElseThrow(() -> new BadRequestException("No worker found with id " + request.assignedWorkerId()));
            if (residentId == null) {
                residentId = worker.getSponsorResidentId();
            } else if (!residentId.equals(worker.getSponsorResidentId())) {
                throw new BadRequestException(
                        "Worker " + request.assignedWorkerId() + " is not sponsored by resident " + residentId);
            }
        }
        if (residentId != null && !residentRepository.existsById(residentId)) {
            throw new BadRequestException("No resident found with id " + residentId);
        }
        if (request.vehicleId() != null && !vehicleRepository.existsById(request.vehicleId())) {
            throw new BadRequestException("No vehicle found with id " + request.vehicleId());
        }

        RfidTag tag = new RfidTag();
        tag.setTagId(request.tagId());
        tag.setAssignedResidentId(residentId);
        tag.setAssignedWorkerId(request.assignedWorkerId());
        tag.setVehicleId(request.vehicleId());
        tag.setStatus(RfidStatus.ACTIVE);
        tag = rfidTagRepository.save(tag);

        auditService.record("RfidTag", tag.getId(), "ISSUE", tag.getTagId());
        return toResponse(tag);
    }

    @Transactional
    public RfidTagResponse setStatus(Long id, RfidStatus status) {
        RfidTag tag = get(id);
        tag.setStatus(status);
        tag = rfidTagRepository.save(tag);
        auditService.record("RfidTag", tag.getId(), "STATUS_" + status, null);
        return toResponse(tag);
    }

    /** Gate-side verification by tag id; only ACTIVE tags grant access. */
    public RfidTagResponse verify(String tagId) {
        RfidTag tag = rfidTagRepository.findByTagId(tagId)
                .orElseThrow(() -> new NotFoundException("No RFID tag found: " + tagId));
        if (tag.getStatus() != RfidStatus.ACTIVE) {
            throw new BadRequestException("RFID tag " + tagId + " is " + tag.getStatus());
        }
        return toResponse(tag);
    }

    public PageResponse<RfidTagResponse> search(Pageable pageable) {
        Page<RfidTag> page = rfidTagRepository.findAll(pageable);

        Map<Long, String> residentNames = residentRepository.findAllById(idsOf(page, RfidTag::getAssignedResidentId))
                .stream().collect(Collectors.toMap(Resident::getId, Resident::getFullName));
        Map<Long, String> workerNames = workerRepository.findAllById(idsOf(page, RfidTag::getAssignedWorkerId))
                .stream().collect(Collectors.toMap(Worker::getId, Worker::getFullName));
        Map<Long, String> vehiclePlates = vehicleRepository.findAllById(idsOf(page, RfidTag::getVehicleId))
                .stream().collect(Collectors.toMap(Vehicle::getId, Vehicle::getPlateNumber));

        return PageResponse.of(page.map(t -> RfidTagResponse.from(t,
                residentNames.get(t.getAssignedResidentId()),
                workerNames.get(t.getAssignedWorkerId()),
                vehiclePlates.get(t.getVehicleId()))));
    }

    private List<Long> idsOf(Page<RfidTag> page, java.util.function.Function<RfidTag, Long> idFn) {
        return page.getContent().stream().map(idFn).filter(Objects::nonNull).distinct().toList();
    }

    private RfidTagResponse toResponse(RfidTag tag) {
        String residentName = tag.getAssignedResidentId() == null ? null
                : residentRepository.findById(tag.getAssignedResidentId()).map(Resident::getFullName).orElse(null);
        String workerName = tag.getAssignedWorkerId() == null ? null
                : workerRepository.findById(tag.getAssignedWorkerId()).map(Worker::getFullName).orElse(null);
        String vehiclePlate = tag.getVehicleId() == null ? null
                : vehicleRepository.findById(tag.getVehicleId()).map(Vehicle::getPlateNumber).orElse(null);
        return RfidTagResponse.from(tag, residentName, workerName, vehiclePlate);
    }

    private RfidTag get(Long id) {
        return rfidTagRepository.findById(id).orElseThrow(() -> NotFoundException.of("RfidTag", id));
    }
}
