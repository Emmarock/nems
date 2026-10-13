package com.nitelestate.complaint.dto;

import com.nitelestate.complaint.ComplaintCategory;
import com.nitelestate.complaint.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComplaintRequest(
        @NotNull ComplaintCategory category,
        @NotBlank String description,
        ComplaintPriority priority
) {
}
