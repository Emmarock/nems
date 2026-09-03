package com.cyrev.nitelestate.accesspolicy.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccessPolicyRequest(
        @NotNull Boolean enforceArrears,
        @NotNull BigDecimal arrearsThreshold
) {
}
