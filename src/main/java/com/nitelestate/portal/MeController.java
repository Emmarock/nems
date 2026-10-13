package com.nitelestate.portal;

import com.nitelestate.billing.AccountService;
import com.nitelestate.billing.dto.AccountBalanceResponse;
import com.nitelestate.billing.dto.InvoiceResponse;
import com.nitelestate.billing.dto.LevyBalanceResponse;
import com.nitelestate.common.dto.PageResponse;
import com.nitelestate.common.search.Paging;
import com.nitelestate.payment.PaymentService;
import com.nitelestate.payment.dto.PaymentReceiptRequest;
import com.nitelestate.payment.dto.PaymentResponse;
import com.nitelestate.payment.dto.SharedReceiptResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.portal.dto.MeDashboardResponse;
import com.nitelestate.property.PropertyService;
import com.nitelestate.property.dto.ApartmentCountRequest;
import com.nitelestate.property.dto.MePropertyUpdateRequest;
import com.nitelestate.property.dto.PropertyResponse;
import com.nitelestate.resident.ResidentService;
import com.nitelestate.resident.dto.MeProfileUpdateRequest;
import com.nitelestate.resident.dto.ResidentResponse;
import com.nitelestate.resident.dto.TenantRequest;
import com.nitelestate.resident.dto.TenantResponse;
import com.nitelestate.security.CurrentUser;
import com.nitelestate.vehicle.VehicleService;
import com.nitelestate.vehicle.dto.MeVehicleRequest;
import com.nitelestate.vehicle.dto.VehicleResponse;
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
     * Self-service correction of the resident's own house number/address/road/postcode. Resolves
     * the property id from the caller's own resident record server-side, so there's nothing for
     * the client to pass that could point at someone else's property.
     */
    @PutMapping("/property")
    public PropertyResponse updateMyProperty(@Valid @RequestBody MePropertyUpdateRequest request) {
        ResidentResponse resident = residentService.findById(currentUser.residentId());
        if (resident.propertyId() == null) {
            throw new BadRequestException("You don't have a property on file yet — contact an admin to get one linked");
        }
        return propertyService.updateSelf(resident.propertyId(), request);
    }

    /**
     * Landlord self-service: how many apartment units the caller's own property has - caps how
     * many ACTIVE tenants they can add (see ResidentService.createTenant). Ownership is resolved
     * server-side by PropertyService (whoever calls this must own the property, not just live on
     * it), same as /tenants below - a tenant calling this gets the same "you don't own a
     * property" error a landlord with no property at all would.
     */
    @PutMapping("/landlord/apartment-count")
    public PropertyResponse updateApartmentCount(@RequestBody ApartmentCountRequest request) {
        return propertyService.updateApartmentCount(currentUser.residentId(), request.apartmentCount());
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

    /**
     * "I paid this levy offline, here's my receipt" - awaits treasurer/financial-secretary review.
     * Landlord/owner only — a tenant isn't billed on their own account (see PaymentService), so
     * for them this throws; they use /me/receipts/share instead.
     */
    @PostMapping("/payments/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse submitReceipt(@Valid @RequestBody PaymentReceiptRequest request) {
        return paymentService.submitReceipt(currentUser.residentId(), request);
    }

    /** Tenant self-service: their landlord's balance, read-only — billing lives on the landlord's account, not theirs. */
    @GetMapping("/landlord/account/balance")
    public AccountBalanceResponse landlordBalance() {
        return accountService.getBalance(residentService.resolveLandlordResidentId(currentUser.residentId()));
    }

    @GetMapping("/landlord/account/balance-breakdown")
    public List<LevyBalanceResponse> landlordBalanceBreakdown() {
        return accountService.getBalanceBreakdown(residentService.resolveLandlordResidentId(currentUser.residentId()));
    }

    @GetMapping("/landlord/account/invoices")
    public PageResponse<InvoiceResponse> landlordInvoices(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        Long landlordId = residentService.resolveLandlordResidentId(currentUser.residentId());
        return accountService.getInvoices(landlordId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "issueDate")));
    }

    @GetMapping("/landlord/account/payments")
    public PageResponse<PaymentResponse> landlordPayments(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        Long landlordId = residentService.resolveLandlordResidentId(currentUser.residentId());
        return accountService.getPayments(landlordId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt")));
    }

    /** What a tenant does instead of submitting a receipt directly — hands it to their landlord. */
    @PostMapping("/receipts/share")
    @ResponseStatus(HttpStatus.CREATED)
    public SharedReceiptResponse shareReceipt(@Valid @RequestBody PaymentReceiptRequest request) {
        return paymentService.shareReceiptWithLandlord(currentUser.residentId(), request);
    }

    /** Landlord self-service: receipts tenants have shared, awaiting review. */
    @GetMapping("/receipts/shared")
    public PageResponse<SharedReceiptResponse> sharedReceipts(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return paymentService.listSharedReceiptsForLandlord(currentUser.residentId(),
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /** Landlord reviewed a shared receipt and confirmed it — submits it as a normal payment under their own account. */
    @PostMapping("/receipts/shared/{id}/submit")
    public PaymentResponse submitSharedReceipt(@PathVariable Long id) {
        return paymentService.submitSharedReceipt(currentUser.residentId(), id);
    }

    /** Landlord decided a shared receipt doesn't need submitting (e.g. a duplicate). */
    @PostMapping("/receipts/shared/{id}/dismiss")
    public void dismissSharedReceipt(@PathVariable Long id) {
        paymentService.dismissSharedReceipt(currentUser.residentId(), id);
    }

    @GetMapping("/vehicles")
    public PageResponse<VehicleResponse> vehicles(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return vehicleService.search(null, currentUser.residentId(), Paging.of(page, size, Sort.by("plateNumber")));
    }

    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse registerVehicle(@Valid @RequestBody MeVehicleRequest request) {
        return vehicleService.registerForResident(currentUser.residentId(), request);
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

    /** Landlord self-service: tenants currently on the caller's own property. */
    @GetMapping("/tenants")
    public PageResponse<ResidentResponse> tenants(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return residentService.listTenants(currentUser.residentId(), Paging.of(page, size, Sort.by("fullName")));
    }

    /** Landlord self-service: add a tenant to the caller's own property and provision their login. */
    @PostMapping("/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse addTenant(@Valid @RequestBody TenantRequest request) {
        return residentService.createTenant(currentUser.residentId(), request);
    }

    /** Landlord self-service: deactivate a tenant on the caller's own property (soft-remove). */
    @PutMapping("/tenants/{id}/deactivate")
    public ResidentResponse deactivateTenant(@PathVariable Long id) {
        return residentService.deactivateTenant(currentUser.residentId(), id);
    }
}
