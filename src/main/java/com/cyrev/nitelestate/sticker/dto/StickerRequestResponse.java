package com.cyrev.nitelestate.sticker.dto;

import com.cyrev.nitelestate.sticker.StickerRequest;
import com.cyrev.nitelestate.sticker.StickerStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record StickerRequestResponse(
        Long id,
        Long vehicleId,
        String plateNumber,
        Long residentId,
        String residentName,
        Long invoiceId,
        BigDecimal invoiceAmount,
        StickerStatus status,
        Instant requestedAt,
        Instant issuedAt
) {
    public static StickerRequestResponse from(StickerRequest sr, String plateNumber, String residentName,
                                               BigDecimal invoiceAmount) {
        return new StickerRequestResponse(sr.getId(), sr.getVehicleId(), plateNumber, sr.getResidentId(),
                residentName, sr.getInvoiceId(), invoiceAmount, sr.getStatus(), sr.getCreatedAt(), sr.getIssuedAt());
    }
}
