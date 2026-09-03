package com.cyrev.nitelestate.worker;

/** Approval lifecycle per Nitel_Estate_Phase_2_Resident_Experience.md §4 (Worker Module). */
public enum WorkerStatus {
    PENDING,
    APPROVED,
    ACTIVE,
    SUSPENDED,
    EXPIRED,
    COMPLETED
}
