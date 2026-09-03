package com.cyrev.nitelestate.gate.dto;

import com.cyrev.nitelestate.gate.GateStatus;
import com.cyrev.nitelestate.gate.GateType;
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
