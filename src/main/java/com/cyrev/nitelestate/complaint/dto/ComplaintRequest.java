package com.cyrev.nitelestate.complaint.dto;

import com.cyrev.nitelestate.complaint.ComplaintCategory;
import com.cyrev.nitelestate.complaint.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComplaintRequest(
        @NotNull ComplaintCategory category,
        @NotBlank String description,
        ComplaintPriority priority
) {
}
