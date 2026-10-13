package com.nitelestate.access.dto;

import com.nitelestate.access.AccessDirection;
import com.nitelestate.access.AccessSubjectType;
import jakarta.validation.constraints.NotNull;

public record AccessEventCreateRequest(
        @NotNull AccessSubjectType subjectType,
        @NotNull Long subjectId,
        @NotNull AccessDirection direction,
        Long gateId
) {
}
