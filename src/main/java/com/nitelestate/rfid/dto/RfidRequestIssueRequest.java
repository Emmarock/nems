package com.nitelestate.rfid.dto;

import jakarta.validation.constraints.NotBlank;

public record RfidRequestIssueRequest(
        @NotBlank String tagId
) {
}
