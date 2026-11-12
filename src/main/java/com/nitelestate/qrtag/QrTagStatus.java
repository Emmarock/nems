package com.nitelestate.qrtag;

/** Derived, never stored - see QrTagService.statusOf. */
public enum QrTagStatus {
    /** Printed, not yet linked to anything. */
    AVAILABLE,
    /** Linked, and still the live pass of its vehicle/property. */
    ASSIGNED,
    /** Was linked, but its vehicle/property has since moved to another token - the sticker no longer works and can't be reused. */
    VOID
}
