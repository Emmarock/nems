package com.nitelestate.accesspolicy;

import com.nitelestate.audit.AuditService;
import com.nitelestate.billing.AccountService;
import com.nitelestate.billing.dto.AccountBalanceResponse;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.vehicle.VehicleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Verifies spec Phase 3 §3: "Deny/flag residents with outstanding balances beyond a threshold." */
@ExtendWith(MockitoExtension.class)
class AccessPolicyServiceTest {

    @Mock
    private AccessPolicySettingsRepository settingsRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private VehicleService vehicleService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AccessPolicyService accessPolicyService;

    @Test
    void flagsResident_whenOutstandingExceedsThreshold() {
        AccessPolicySettings settings = new AccessPolicySettings();
        settings.setEnforceArrears(true);
        settings.setArrearsThreshold(new BigDecimal("10000.00"));
        when(settingsRepository.findAll()).thenReturn(List.of(settings));
        when(accountService.getBalance(1L)).thenReturn(
                new AccountBalanceResponse(1L, new BigDecimal("50000"), new BigDecimal("0"),
                        BigDecimal.ZERO, new BigDecimal("50000")));

        assertThat(accessPolicyService.evaluateResident(1L)).isEqualTo("ACCOUNT_IN_ARREARS");
    }

    @Test
    void doesNotFlagResident_whenWithinThreshold() {
        AccessPolicySettings settings = new AccessPolicySettings();
        settings.setEnforceArrears(true);
        settings.setArrearsThreshold(new BigDecimal("10000.00"));
        when(settingsRepository.findAll()).thenReturn(List.of(settings));
        when(accountService.getBalance(2L)).thenReturn(
                new AccountBalanceResponse(2L, new BigDecimal("5000"), new BigDecimal("0"),
                        BigDecimal.ZERO, new BigDecimal("5000")));

        assertThat(accessPolicyService.evaluateResident(2L)).isNull();
    }

    @Test
    void doesNotFlag_whenEnforcementDisabled() {
        AccessPolicySettings settings = new AccessPolicySettings();
        settings.setEnforceArrears(false);
        settings.setArrearsThreshold(BigDecimal.ZERO);
        when(settingsRepository.findAll()).thenReturn(List.of(settings));

        assertThat(accessPolicyService.evaluateResident(3L)).isNull();
    }

    @Test
    void requireSponsorEligibleForEntry_throws_whenSponsorOutstandingExceedsThreshold() {
        AccessPolicySettings settings = new AccessPolicySettings();
        settings.setEnforceArrears(true);
        settings.setArrearsThreshold(new BigDecimal("10000.00"));
        when(settingsRepository.findAll()).thenReturn(List.of(settings));
        when(accountService.getBalance(4L)).thenReturn(
                new AccountBalanceResponse(4L, new BigDecimal("50000"), new BigDecimal("0"),
                        BigDecimal.ZERO, new BigDecimal("50000")));

        assertThatThrownBy(() -> accessPolicyService.requireSponsorEligibleForEntry(4L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireSponsorEligibleForEntry_doesNotThrow_whenSponsorWithinThreshold() {
        AccessPolicySettings settings = new AccessPolicySettings();
        settings.setEnforceArrears(true);
        settings.setArrearsThreshold(new BigDecimal("10000.00"));
        when(settingsRepository.findAll()).thenReturn(List.of(settings));
        when(accountService.getBalance(5L)).thenReturn(
                new AccountBalanceResponse(5L, new BigDecimal("5000"), new BigDecimal("0"),
                        BigDecimal.ZERO, new BigDecimal("5000")));

        assertThatCode(() -> accessPolicyService.requireSponsorEligibleForEntry(5L)).doesNotThrowAnyException();
    }

    @Test
    void requireSponsorEligibleForEntry_doesNotThrow_whenEnforcementDisabled() {
        AccessPolicySettings settings = new AccessPolicySettings();
        settings.setEnforceArrears(false);
        settings.setArrearsThreshold(BigDecimal.ZERO);
        when(settingsRepository.findAll()).thenReturn(List.of(settings));

        assertThatCode(() -> accessPolicyService.requireSponsorEligibleForEntry(6L)).doesNotThrowAnyException();
    }
}
