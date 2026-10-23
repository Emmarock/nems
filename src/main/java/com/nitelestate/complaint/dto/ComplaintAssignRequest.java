package com.nitelestate.complaint.dto;

import com.nitelestate.user.Role;

/** assignedRole is nullable - null clears the assignment. */
public record ComplaintAssignRequest(Role assignedRole) {
}
