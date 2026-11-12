package com.nitelestate.qrtag.dto;

import com.nitelestate.qrtag.QrTagKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** {@code code} is the printed code (V-000123, case and dash optional), the scanned QR URL, or the raw token. */
public record QrTagAssignRequest(@NotNull QrTagKind kind, @NotNull Long subjectId, @NotBlank String code) {
}
