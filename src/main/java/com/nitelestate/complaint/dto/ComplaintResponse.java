package com.nitelestate.complaint.dto;

import com.nitelestate.complaint.Complaint;
import com.nitelestate.complaint.ComplaintCategory;
import com.nitelestate.complaint.ComplaintPriority;
import com.nitelestate.complaint.ComplaintStatus;

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
