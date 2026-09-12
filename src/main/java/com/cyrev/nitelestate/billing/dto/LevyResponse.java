package com.cyrev.nitelestate.billing.dto;

import com.cyrev.nitelestate.billing.Levy;
import com.cyrev.nitelestate.billing.LevyFrequency;

import java.math.BigDecimal;

public record LevyResponse(Long id, String name, BigDecimal amount, LevyFrequency frequency, boolean active,
                            boolean vehicleStickerLevy, Long paymentAccountId, String paymentAccountLabel) {
    public static LevyResponse from(Levy l, String paymentAccountLabel) {
        return new LevyResponse(l.getId(), l.getName(), l.getAmount(), l.getFrequency(), l.isActive(),
                l.isVehicleStickerLevy(), l.getPaymentAccountId(), paymentAccountLabel);
    }
}
