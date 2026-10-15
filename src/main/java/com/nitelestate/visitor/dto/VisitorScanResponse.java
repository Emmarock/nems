package com.nitelestate.visitor.dto;

import com.nitelestate.access.AccessDirection;

/** What a gate scan of a visitor's QR pass actually did — the token itself fixed the direction,
 * not whoever scanned it, so the caller can't ask for the other one (see VisitorService.scan). */
public record VisitorScanResponse(
        VisitorResponse visitor,
        AccessDirection direction
) {
}
