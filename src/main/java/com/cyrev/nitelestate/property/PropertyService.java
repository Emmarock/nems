package com.cyrev.nitelestate.property;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.billing.AccountService;
import com.cyrev.nitelestate.billing.dto.AccountBalanceResponse;
import com.cyrev.nitelestate.billing.dto.LevyBalanceResponse;
import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.exception.ConflictException;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.common.search.Specs;
import com.cyrev.nitelestate.payment.PaymentService;
import com.cyrev.nitelestate.payment.dto.PaymentResponse;
import com.cyrev.nitelestate.property.dto.MePropertyUpdateRequest;
import com.cyrev.nitelestate.property.dto.PropertyLookupResponse;
import com.cyrev.nitelestate.property.dto.PropertyRequest;
import com.cyrev.nitelestate.property.dto.PropertyResponse;
import com.cyrev.nitelestate.resident.Resident;
import com.cyrev.nitelestate.resident.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final ResidentRepository residentRepository;
    private final AccountService accountService;
    private final PaymentService paymentService;
    private final AuditService auditService;

    @Transactional
    public PropertyResponse create(PropertyRequest request) {
        if (propertyRepository.existsByHouseNumberIgnoreCase(request.houseNumber())) {
            throw new ConflictException("A property with house number " + request.houseNumber() + " already exists");
        }
        Property property = new Property();
        apply(property, request);
        property = propertyRepository.save(property);
        auditService.record("Property", property.getId(), "CREATE", property.getHouseNumber());
        return PropertyResponse.from(property, resolveOwnerName(property.getOwnerId()));
    }

    @Transactional
    public PropertyResponse update(Long id, PropertyRequest request) {
        Property property = get(id);
        apply(property, request);
        property = propertyRepository.save(property);
        auditService.record("Property", property.getId(), "UPDATE", property.getHouseNumber());
        return PropertyResponse.from(property, resolveOwnerName(property.getOwnerId()));
    }

    public PropertyResponse findById(Long id) {
        Property property = get(id);
        return PropertyResponse.from(property, resolveOwnerName(property.getOwnerId()));
    }

    /**
     * Self-service correction of a resident's own house number/address — the id is resolved
     * server-side from the caller's own resident.propertyId (see MeController), never accepted
     * from the client, so there's no separate ownership check to get wrong here.
     */
    @Transactional
    public PropertyResponse updateSelf(Long propertyId, MePropertyUpdateRequest request) {
        Property property = get(propertyId);
        if (!property.getHouseNumber().equalsIgnoreCase(request.houseNumber())
                && propertyRepository.existsByHouseNumberIgnoreCase(request.houseNumber())) {
            throw new ConflictException("A property with house number " + request.houseNumber() + " already exists");
        }
        property.setHouseNumber(request.houseNumber());
        if (request.address() != null && !request.address().isBlank()) {
            property.setAddress(request.address());
        }
        property = propertyRepository.save(property);
        auditService.record("Property", property.getId(), "SELF_UPDATE", property.getHouseNumber());
        return PropertyResponse.from(property, resolveOwnerName(property.getOwnerId()));
    }

    public PageResponse<PropertyResponse> search(String q, Pageable pageable) {
        Specification<Property> spec = Specs.contains(q, "houseNumber", "block", "plot", "address");
        var page = propertyRepository.findAll(spec, pageable);

        List<Long> ownerIds = page.getContent().stream().map(Property::getOwnerId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> ownerNames = residentRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(Resident::getId, Resident::getFullName));

        return PageResponse.of(page.map(p -> PropertyResponse.from(p, ownerNames.get(p.getOwnerId()))));
    }

    private String resolveOwnerName(Long ownerId) {
        return ownerId == null ? null : residentRepository.findById(ownerId).map(Resident::getFullName).orElse(null);
    }

    /** A building's enforcement QR pass, generated the first time it's asked for. */
    @Transactional
    public String getOrCreateQrToken(Long propertyId) {
        Property property = get(propertyId);
        if (property.getQrToken() == null) {
            property.setQrToken(UUID.randomUUID().toString());
            property = propertyRepository.save(property);
            auditService.record("Property", property.getId(), "ISSUE_ACCESS_PASS", null);
        }
        return property.getQrToken();
    }

    /**
     * Read-only enforcement scan: resolves the building's owner and their full payment picture
     * (balance, per-levy breakdown, recent payments) so compliance can be checked on the spot.
     */
    public PropertyLookupResponse lookup(String qrToken) {
        Property property = propertyRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new NotFoundException("No building pass found for this QR code"));

        Resident owner = property.getOwnerId() != null
                ? residentRepository.findById(property.getOwnerId()).orElse(null) : null;

        AccountBalanceResponse balance = null;
        List<LevyBalanceResponse> levyBreakdown = List.of();
        List<PaymentResponse> recentPayments = List.of();
        if (owner != null) {
            balance = accountService.getBalance(owner.getId());
            levyBreakdown = accountService.getBalanceBreakdown(owner.getId());
            recentPayments = paymentService.search(null, owner.getId(), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "paidAt")))
                    .content();
        }

        return new PropertyLookupResponse(property.getId(), property.getHouseNumber(), property.getBlock(),
                property.getPlot(), property.getAddress(), property.getPropertyType(), property.getOccupancyStatus(),
                property.getOwnerId(), owner != null ? owner.getFullName() : null, owner != null ? owner.getPhone() : null,
                balance, levyBreakdown, recentPayments);
    }

    Property get(Long id) {
        return propertyRepository.findById(id).orElseThrow(() -> NotFoundException.of("Property", id));
    }

    private void apply(Property property, PropertyRequest request) {
        property.setBlock(request.block());
        property.setPlot(request.plot());
        property.setHouseNumber(request.houseNumber());
        property.setAddress(request.address());
        property.setPropertyType(request.propertyType());
        property.setOwnerId(request.ownerId());
        if (request.occupancyStatus() != null) {
            property.setOccupancyStatus(request.occupancyStatus());
        }
    }
}
