package com.cyrev.nitelestate.complaint.dto;

import com.cyrev.nitelestate.complaint.ComplaintStatus;
import jakarta.validation.constraints.NotNull;

public record ComplaintUpdateRequest(
        @NotNull ComplaintStatus status,
        String assignedTo
) {
}
