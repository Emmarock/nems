package com.cyrev.nitelestate.access.dto;

import com.cyrev.nitelestate.access.AccessDirection;
import com.cyrev.nitelestate.access.AccessSubjectType;
import jakarta.validation.constraints.NotNull;

public record AccessEventCreateRequest(
        @NotNull AccessSubjectType subjectType,
        @NotNull Long subjectId,
        @NotNull AccessDirection direction,
        Long gateId
) {
}
