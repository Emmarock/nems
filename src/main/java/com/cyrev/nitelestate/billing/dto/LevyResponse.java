package com.cyrev.nitelestate.billing.dto;

import com.cyrev.nitelestate.billing.Levy;
import com.cyrev.nitelestate.billing.LevyFrequency;

import java.math.BigDecimal;

public record LevyResponse(Long id, String name, BigDecimal amount, LevyFrequency frequency, boolean active) {
    public static LevyResponse from(Levy l) {
        return new LevyResponse(l.getId(), l.getName(), l.getAmount(), l.getFrequency(), l.isActive());
    }
}
