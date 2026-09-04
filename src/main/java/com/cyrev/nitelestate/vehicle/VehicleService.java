package com.cyrev.nitelestate.vehicle;

import com.cyrev.nitelestate.access.AccessDirection;
import com.cyrev.nitelestate.access.AccessEventService;
import com.cyrev.nitelestate.access.AccessSubjectType;
import com.cyrev.nitelestate.accesspolicy.AccessPolicyService;
import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.ConflictException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.property.Property;
import com.cyrev.nitelestate.property.PropertyRepository;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.vehicle.dto.VehicleLookupResponse;
import com.cyrev.nitelestate.vehicle.dto.VehicleRequest;
import com.cyrev.nitelestate.vehicle.dto.VehicleResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ResidentRepository residentRepository;
    private final PropertyRepository propertyRepository;
    private final AccessEventService accessEventService;
    private final AccessPolicyService accessPolicyService;
    private final AuditService auditService;

    /**
     * {@code @Lazy} on accessPolicyService breaks a circular bean dependency: AccessPolicyService
     * already depends on VehicleService (its own pre-existing evaluateVehicle() helper), so a
     * plain eager injection here would form a cycle neither Spring nor a manual constructor
     * ordering can resolve. The lazy proxy defers actually resolving that bean until it's first
     * called (in evaluateVehicle() below), by which point both beans exist.
     */
    public VehicleService(VehicleRepository vehicleRepository, ResidentRepository residentRepository,
                           PropertyRepository propertyRepository, AccessEventService accessEventService,
                           @Lazy AccessPolicyService accessPolicyService, AuditService auditService) {
        this.vehicleRepository = vehicleRepository;
        this.residentRepository = residentRepository;
        this.propertyRepository = propertyRepository;
        this.accessEventService = accessEventService;
        this.accessPolicyService = accessPolicyService;
        this.auditService = auditService;
    }

    @Transactional
    public VehicleResponse register(VehicleRequest request) {
        if (vehicleRepository.existsByPlateNumberIgnoreCase(request.plateNumber())) {
            throw new ConflictException("A vehicle with plate " + request.plateNumber() + " is already registered");
        }
        Vehicle vehicle = new Vehicle();
        apply(vehicle, request);
        vehicle = vehicleRepository.save(vehicle);
        auditService.record("Vehicle", vehicle.getId(), "CREATE", vehicle.getPlateNumber());
        return VehicleResponse.from(vehicle, resolveResidentName(vehicle.getResidentId()));
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest request) {
        Vehicle vehicle = get(id);
        apply(vehicle, request);
        vehicle = vehicleRepository.save(vehicle);
        auditService.record("Vehicle", vehicle.getId(), "UPDATE", vehicle.getPlateNumber());
        return VehicleResponse.from(vehicle, resolveResidentName(vehicle.getResidentId()));
    }

    public VehicleResponse findById(Long id) {
        Vehicle vehicle = get(id);
        return VehicleResponse.from(vehicle, resolveResidentName(vehicle.getResidentId()));
    }

    public VehicleResponse findByPlate(String plate) {
        Vehicle vehicle = vehicleRepository.findByPlateNumberIgnoreCase(plate)
                .orElseThrow(() -> NotFoundException.of("Vehicle", plate));
        return VehicleResponse.from(vehicle, resolveResidentName(vehicle.getResidentId()));
    }

    public PageResponse<VehicleResponse> search(String q, Long residentId, Pageable pageable) {
        Specification<Vehicle> spec = Specification.<Vehicle>where(Specs.contains(q, "plateNumber", "make", "model", "colour"))
                .and(Specs.eq(residentId, "residentId"));
        var page = vehicleRepository.findAll(spec, pageable);

        List<Long> residentIds = page.getContent().stream().map(Vehicle::getResidentId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> residentNames = residentRepository.findAllById(residentIds).stream()
                .collect(Collectors.toMap(Resident::getId, Resident::getFullName));

        return PageResponse.of(page.map(v -> VehicleResponse.from(v, residentNames.get(v.getResidentId()))));
    }

    /** Admin-issued QR pass for any vehicle — for printing/handing out, or for an enforcement lookup. No ownership check. */
    @Transactional
    public String getOrCreateQrToken(Long vehicleId) {
        Vehicle vehicle = get(vehicleId);
        if (vehicle.getQrToken() == null) {
            vehicle.setQrToken(UUID.randomUUID().toString());
            vehicle = vehicleRepository.save(vehicle);
            auditService.record("Vehicle", vehicle.getId(), "ISSUE_ACCESS_PASS", null);
        }
        return vehicle.getQrToken();
    }

    /**
     * A resident's own QR pass for one of their registered vehicles. Scoped to the caller's own
     * vehicles — the id comes straight from the URL, unlike registerVehicle()'s resident id, so
     * ownership has to be checked here (getOrCreateQrToken has no such check, by design).
     */
    @Transactional
    public String getOrCreateQrTokenForResident(Long vehicleId, Long residentId) {
        Vehicle vehicle = get(vehicleId);
        if (!vehicle.getResidentId().equals(residentId)) {
            throw new BadRequestException("You can only view the QR pass for your own vehicle");
        }
        return getOrCreateQrToken(vehicleId);
    }

    /**
     * Gate-side QR scan: identifies the vehicle and its owning resident (destination) before
     * security decides whether to grant access. Read-only, no side effects. The flag mirrors
     * what a resident scan itself would show (arrears etc.) — a vehicle's "good standing" rides
     * on its owner's account — plus a vehicle-specific INACTIVE check on top.
     */
    public VehicleLookupResponse lookup(String qrToken) {
        Vehicle vehicle = vehicleRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No vehicle pass found for this QR code"));
        Resident resident = residentRepository.findById(vehicle.getResidentId()).orElse(null);
        String flag = evaluateVehicle(vehicle, resident);

        Property property = resident != null && resident.getPropertyId() != null
                ? propertyRepository.findById(resident.getPropertyId()).orElse(null) : null;

        return new VehicleLookupResponse(vehicle.getId(), vehicle.getPlateNumber(), vehicle.getVehicleType(),
                vehicle.getMake(), vehicle.getModel(), vehicle.getColour(), vehicle.getStatus(), flag,
                vehicle.getResidentId(), resident != null ? resident.getFullName() : null,
                resident != null ? resident.getPhone() : null,
                property != null ? property.getId() : null, property != null ? property.getHouseNumber() : null,
                property != null ? property.getAddress() : null);
    }

    /** Gate-side check-in; flags an inactive vehicle or an owner in arrears rather than blocking outright. */
    @Transactional
    public VehicleResponse checkIn(String qrToken, Long gateId, Long verifiedByUserId) {
        Vehicle vehicle = vehicleRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No vehicle pass found for this QR code"));
        Resident resident = residentRepository.findById(vehicle.getResidentId()).orElse(null);
        String flag = evaluateVehicle(vehicle, resident);

        accessEventService.record(AccessSubjectType.VEHICLE, vehicle.getId(), AccessDirection.IN, gateId,
                verifiedByUserId, flag);
        auditService.record("Vehicle", vehicle.getId(), "CHECK_IN", flag);
        return VehicleResponse.from(vehicle, resident != null ? resident.getFullName() : null);
    }

    @Transactional
    public VehicleResponse checkOut(String qrToken, Long gateId, Long verifiedByUserId) {
        Vehicle vehicle = vehicleRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No vehicle pass found for this QR code"));

        accessEventService.record(AccessSubjectType.VEHICLE, vehicle.getId(), AccessDirection.OUT, gateId,
                verifiedByUserId, null);
        auditService.record("Vehicle", vehicle.getId(), "CHECK_OUT", null);
        return VehicleResponse.from(vehicle, resolveResidentName(vehicle.getResidentId()));
    }

    private String resolveResidentName(Long residentId) {
        return residentId == null ? null : residentRepository.findById(residentId).map(Resident::getFullName).orElse(null);
    }

    private String evaluateVehicle(Vehicle vehicle, Resident resident) {
        if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
            return "VEHICLE_INACTIVE";
        }
        return resident != null ? accessPolicyService.evaluateResident(resident.getId()) : null;
    }

    Vehicle get(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> NotFoundException.of("Vehicle", id));
    }

    private void apply(Vehicle vehicle, VehicleRequest request) {
        if (!residentRepository.existsById(request.residentId())) {
            throw new BadRequestException("No resident found with id " + request.residentId());
        }
        vehicle.setPlateNumber(request.plateNumber().toUpperCase());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        vehicle.setColour(request.colour());
        vehicle.setResidentId(request.residentId());
    }
}
