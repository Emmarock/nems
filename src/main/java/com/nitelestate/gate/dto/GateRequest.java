package com.nitelestate.gate.dto;

import com.nitelestate.gate.GateStatus;
import com.nitelestate.gate.GateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GateRequest(
        @NotBlank String name,
        @NotBlank String code,
        String location,
        @NotNull GateType type,
        GateStatus status
) {
}
