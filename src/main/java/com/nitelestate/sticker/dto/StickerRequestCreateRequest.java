package com.nitelestate.sticker.dto;

import jakarta.validation.constraints.NotNull;

public record StickerRequestCreateRequest(@NotNull Long vehicleId) {
}
