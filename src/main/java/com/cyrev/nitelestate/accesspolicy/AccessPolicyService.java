package com.cyrev.nitelestate.accesspolicy;

import com.cyrev.nitelestate.accesspolicy.dto.AccessPolicyRequest;
import com.cyrev.nitelestate.accesspolicy.dto.AccessPolicyResponse;
import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.billing.AccountService;
import com.cyrev.nitelestate.billing.dto.AccountBalanceResponse;
import com.cyrev.nitelestate.vehicle.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessPolicyService {

    private final AccessPolicySettingsRepository settingsRepository;
    private final AccountService accountService;
    private final VehicleService vehicleService;
    private final AuditService auditService;

    public AccessPolicyResponse getSettings() {
        return AccessPolicyResponse.from(getOrCreate());
    }

    @Transactional
    public AccessPolicyResponse updateSettings(AccessPolicyRequest request) {
        AccessPolicySettings settings = getOrCreate();
        settings.setEnforceArrears(request.enforceArrears());
        settings.setArrearsThreshold(request.arrearsThreshold());
        settings = settingsRepository.save(settings);
        auditService.record("AccessPolicySettings", settings.getId(), "UPDATE",
                "enforceArrears=" + settings.isEnforceArrears() + " threshold=" + settings.getArrearsThreshold());
        return AccessPolicyResponse.from(settings);
    }

    /** Spec Phase 3 §3: "Deny/flag residents with outstanding balances beyond a threshold." */
    public String evaluateResident(Long residentId) {
        AccessPolicySettings settings = getOrCreate();
        if (!settings.isEnforceArrears()) {
            return null;
        }
        AccountBalanceResponse balance = accountService.getBalance(residentId);
        return balance.outstanding().compareTo(settings.getArrearsThreshold()) > 0 ? "ACCOUNT_IN_ARREARS" : null;
    }

    public String evaluateVehicle(Long vehicleId) {
        Long residentId = vehicleService.findById(vehicleId).residentId();
        return evaluateResident(residentId);
    }

    private AccessPolicySettings getOrCreate() {
        return settingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> settingsRepository.save(new AccessPolicySettings()));
    }
}
