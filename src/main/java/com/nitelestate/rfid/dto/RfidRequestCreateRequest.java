package com.nitelestate.rfid.dto;

import com.nitelestate.access.AccessSubjectType;
import jakarta.validation.constraints.NotNull;

public record RfidRequestCreateRequest(
        @NotNull AccessSubjectType subjectType,
        @NotNull Long subjectId
) {
}
