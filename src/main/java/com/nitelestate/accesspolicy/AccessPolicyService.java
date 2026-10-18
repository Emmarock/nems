package com.nitelestate.accesspolicy;

import com.nitelestate.accesspolicy.dto.AccessPolicyRequest;
import com.nitelestate.accesspolicy.dto.AccessPolicyResponse;
import com.nitelestate.audit.AuditService;
import com.nitelestate.billing.AccountService;
import com.nitelestate.billing.dto.AccountBalanceResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.vehicle.VehicleService;
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

    /**
     * Hard block (throws), unlike evaluateResident's advisory flag - a sponsoring resident's
     * guests, household members, and workers are refused ENTRY outright (not just flagged) once
     * the sponsor owes beyond the policy threshold. Checked only on check-in: once someone is
     * already inside the estate, letting them leave is never blocked by this policy.
     */
    public void requireSponsorEligibleForEntry(Long sponsorResidentId) {
        AccessPolicySettings settings = getOrCreate();
        if (!settings.isEnforceArrears() || sponsorResidentId == null) {
            return;
        }
        AccountBalanceResponse balance = accountService.getBalance(sponsorResidentId);
        if (balance.outstanding().compareTo(settings.getArrearsThreshold()) > 0) {
            throw new BadRequestException(
                    "Entry denied: the resident this pass is sponsored by has outstanding arrears beyond the estate's access policy threshold");
        }
    }

    private AccessPolicySettings getOrCreate() {
        return settingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> settingsRepository.save(new AccessPolicySettings()));
    }
}
