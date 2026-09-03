package com.cyrev.nitelestate.complaint.dto;

import com.cyrev.nitelestate.complaint.Complaint;
import com.cyrev.nitelestate.complaint.ComplaintCategory;
import com.cyrev.nitelestate.complaint.ComplaintPriority;
import com.cyrev.nitelestate.complaint.ComplaintStatus;

import java.time.Instant;

public record ComplaintResponse(
        Long id,
        Long residentId,
        ComplaintCategory category,
        String description,
        ComplaintStatus status,
        ComplaintPriority priority,
        String assignedTo,
        Instant createdAt,
        Instant resolvedAt
) {
    public static ComplaintResponse from(Complaint c) {
        return new ComplaintResponse(c.getId(), c.getResidentId(), c.getCategory(), c.getDescription(), c.getStatus(),
                c.getPriority(), c.getAssignedTo(), c.getCreatedAt(), c.getResolvedAt());
    }
}
