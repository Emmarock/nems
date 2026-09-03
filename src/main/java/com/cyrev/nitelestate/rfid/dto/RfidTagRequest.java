package com.cyrev.nitelestate.rfid.dto;

import jakarta.validation.constraints.NotBlank;

public record RfidTagRequest(
        @NotBlank String tagId,
        Long assignedResidentId,
        Long assignedWorkerId,
        Long vehicleId
) {
}
