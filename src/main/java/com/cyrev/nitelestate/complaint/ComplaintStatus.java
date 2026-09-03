package com.cyrev.nitelestate.complaint;

/** OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED (spec §7). */
public enum ComplaintStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
