package com.cyrev.nitelestate.billing.dto;

import com.cyrev.nitelestate.billing.LevyFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LevyRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        @NotNull LevyFrequency frequency,
        Boolean active
) {
}
