package com.nitelestate.qrtag.dto;

import com.nitelestate.qrtag.QrTagKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QrTagBatchRequest(@NotNull QrTagKind kind, @NotNull @Min(1) @Max(1000) Integer count) {
}
