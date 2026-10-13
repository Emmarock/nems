package com.nitelestate.vehicle;

import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.search.Paging;
import com.nitelestate.security.CurrentUser;
import com.nitelestate.user.Role;
import com.nitelestate.vehicle.dto.VehicleLookupResponse;
import com.nitelestate.vehicle.dto.VehicleRequest;
import com.nitelestate.vehicle.dto.VehicleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY')")
    public VehicleResponse register(@Valid @RequestBody VehicleRequest request) {
        return vehicleService.register(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY')")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(id, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECURITY')")
    public VehicleResponse findById(@PathVariable Long id) {
        return vehicleService.findById(id);
    }

    @GetMapping("/plate/{plate}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECURITY')")
    public VehicleResponse findByPlate(@PathVariable String plate) {
        return vehicleService.findByPlate(plate);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECRETARY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECURITY')")
    public PageResponse<VehicleResponse> findAll(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) Long residentId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return vehicleService.search(q, residentId, Paging.of(page, size, Sort.by("plateNumber")));
    }

    /** Landlord self-service: tenant-registered vehicles on the caller's own property awaiting approval. */
    @GetMapping("/pending-approval")
    @PreAuthorize("hasRole('RESIDENT')")
    public PageResponse<VehicleResponse> pendingApproval(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return vehicleService.pendingForLandlord(currentUser.residentId(), Paging.of(page, size, Sort.by("plateNumber")));
    }

    /** The landlord (found by ownership of the vehicle owner's property) or an admin approves a pending vehicle. */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('RESIDENT', 'SUPER_ADMIN', 'CDA_ADMIN')")
    public VehicleResponse approve(@PathVariable Long id) {
        Long callerResidentId = currentUser.role() == Role.RESIDENT ? currentUser.residentId() : null;
        return vehicleService.approve(id, callerResidentId, currentUser.role());
    }

    /** Admin-issued QR pass for a vehicle — for printing/handing out, or for enforcement lookups. */
    @GetMapping("/{id}/access-pass")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public Map<String, String> accessPass(@PathVariable Long id) {
        return Map.of("qrToken", vehicleService.getOrCreateQrToken(id));
    }

    /** Gate-side QR scan: identifies the vehicle and its owner before deciding to grant access (spec §9). */
    @GetMapping("/lookup/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECRETARY', 'CDA_ADMIN')")
    public VehicleLookupResponse lookup(@PathVariable String qrToken) {
        return vehicleService.lookup(qrToken);
    }

    @PostMapping("/checkin/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public VehicleResponse checkIn(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return vehicleService.checkIn(qrToken, gateId, currentUser.userId());
    }

    @PostMapping("/checkout/{qrToken}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
    public VehicleResponse checkOut(@PathVariable String qrToken, @RequestParam(required = false) Long gateId) {
        return vehicleService.checkOut(qrToken, gateId, currentUser.userId());
    }
}
