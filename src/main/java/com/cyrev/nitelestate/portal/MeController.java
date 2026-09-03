package com.cyrev.nitelestate.portal;

import com.cyrev.nitelestate.billing.AccountService;
import com.cyrev.nitelestate.billing.dto.AccountBalanceResponse;
import com.cyrev.nitelestate.billing.dto.InvoiceResponse;
import com.cyrev.nitelestate.billing.dto.LevyBalanceResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.payment.PaymentService;
import com.cyrev.nitelestate.payment.dto.OnlinePaymentInitiateRequest;
import com.cyrev.nitelestate.payment.dto.OnlinePaymentInitiateResponse;
import com.cyrev.nitelestate.payment.dto.PaymentResponse;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.portal.dto.MeDashboardResponse;
import com.cyrev.nitelestate.property.PropertyService;
import com.cyrev.nitelestate.property.dto.MePropertyUpdateRequest;
import com.cyrev.nitelestate.property.dto.PropertyResponse;
import com.cyrev.nitelestate.resident.ResidentService;
import com.cyrev.nitelestate.resident.dto.MeProfileUpdateRequest;
import com.cyrev.nitelestate.resident.dto.ResidentResponse;
import com.cyrev.nitelestate.security.CurrentUser;
import com.cyrev.nitelestate.vehicle.VehicleService;
import com.cyrev.nitelestate.vehicle.dto.MeVehicleRequest;
import com.cyrev.nitelestate.vehicle.dto.VehicleRequest;
import com.cyrev.nitelestate.vehicle.dto.VehicleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Resident self-service surface (spec §6 Resident Portal) — every endpoint is scoped to the caller's own resident id. */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class MeController {

    private final ResidentService residentService;
    private final PropertyService propertyService;
    private final AccountService accountService;
    private final VehicleService vehicleService;
    private final PaymentService paymentService;
    private final CurrentUser currentUser;

    @GetMapping
    public MeDashboardResponse dashboard() {
        Long residentId = currentUser.residentId();
        ResidentResponse resident = residentService.findById(residentId);
        PropertyResponse property = resident.propertyId() != null ? propertyService.findById(resident.propertyId()) : null;
        AccountBalanceResponse account = accountService.getBalance(residentId);
        var vehicles = vehicleService.search(null, residentId, Paging.of(0, 100, Sort.by("plateNumber"))).content();
        return new MeDashboardResponse(resident, property, account, vehicles);
    }

    /** Self-service profile edit — name/phone/email/emergency contact only, see MeProfileUpdateRequest. */
    @PutMapping("/profile")
    public ResidentResponse updateProfile(@Valid @RequestBody MeProfileUpdateRequest request) {
        return residentService.updateSelf(currentUser.residentId(), request);
    }

    /**
     * Self-service correction of the resident's own house number/address. Resolves the property
     * id from the caller's own resident record server-side, so there's nothing for the client to
     * pass that could point at someone else's property.
     */
    @PutMapping("/property")
    public PropertyResponse updateMyProperty(@Valid @RequestBody MePropertyUpdateRequest request) {
        ResidentResponse resident = residentService.findById(currentUser.residentId());
        if (resident.propertyId() == null) {
            throw new BadRequestException("You don't have a property on file yet — contact an admin to get one linked");
        }
        return propertyService.updateSelf(resident.propertyId(), request);
    }

    @GetMapping("/account/balance")
    public AccountBalanceResponse balance() {
        return accountService.getBalance(currentUser.residentId());
    }

    @GetMapping("/account/balance-breakdown")
    public List<LevyBalanceResponse> balanceBreakdown() {
        return accountService.getBalanceBreakdown(currentUser.residentId());
    }

    @GetMapping("/account/invoices")
    public PageResponse<InvoiceResponse> invoices(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return accountService.getInvoices(currentUser.residentId(), Paging.of(page, size, Sort.by(Sort.Direction.DESC, "issueDate")));
    }

    @GetMapping("/account/payments")
    public PageResponse<PaymentResponse> payments(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return accountService.getPayments(currentUser.residentId(), Paging.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt")));
    }

    @PostMapping("/payments/initiate")
    public OnlinePaymentInitiateResponse payOutstanding(@Valid @RequestBody OnlinePaymentInitiateRequest request) {
        return paymentService.initiateOnline(currentUser.residentId(), request);
    }

    @GetMapping("/vehicles")
    public PageResponse<VehicleResponse> vehicles(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return vehicleService.search(null, currentUser.residentId(), Paging.of(page, size, Sort.by("plateNumber")));
    }

    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse registerVehicle(@Valid @RequestBody MeVehicleRequest request) {
        VehicleRequest scoped = new VehicleRequest(request.plateNumber(), request.vehicleType(), request.make(),
                request.model(), request.colour(), currentUser.residentId());
        return vehicleService.register(scoped);
    }

    /** The resident's own gate-access QR pass — present it at the gate to be scanned as yourself. */
    @GetMapping("/access-pass")
    public Map<String, String> accessPass() {
        return Map.of("qrToken", residentService.getOrCreateQrToken(currentUser.residentId()));
    }

    /** QR pass for one of the resident's own registered vehicles. */
    @GetMapping("/vehicles/{id}/access-pass")
    public Map<String, String> vehicleAccessPass(@PathVariable Long id) {
        return Map.of("qrToken", vehicleService.getOrCreateQrTokenForResident(id, currentUser.residentId()));
    }
}
