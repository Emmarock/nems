package com.nitelestate.complaint.dto;

import com.nitelestate.complaint.ComplaintStatus;
import jakarta.validation.constraints.NotNull;

public record ComplaintUpdateRequest(
        @NotNull ComplaintStatus status
) {
}
