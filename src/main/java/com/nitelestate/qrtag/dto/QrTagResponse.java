package com.nitelestate.qrtag.dto;

import com.nitelestate.qrtag.QrTagKind;
import com.nitelestate.qrtag.QrTagStatus;

import java.time.Instant;

public record QrTagResponse(Long id, QrTagKind kind, String code, int batchNo, QrTagStatus status,
                            Long subjectId, Instant assignedAt) {
}
