package com.cyrev.nitelestate.worker;

/**
 * Worker Module lifecycle (Nitel_Estate_Phase_2_Resident_Experience.md §4). PENDING is retained
 * only for any pre-existing rows from before administrator approval was removed - WorkerService
 * no longer sets it; a new request goes straight to APPROVED (QR issued immediately), matching
 * the Visitor Module's no-approval flow.
 */
public enum WorkerStatus {
    PENDING,
    APPROVED,
    ACTIVE,
    SUSPENDED,
    EXPIRED,
    COMPLETED
}
