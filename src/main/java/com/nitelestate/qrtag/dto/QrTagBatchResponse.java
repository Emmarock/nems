package com.nitelestate.qrtag.dto;

import com.nitelestate.qrtag.QrTagKind;

import java.time.Instant;

public record QrTagBatchResponse(int batchNo, QrTagKind kind, long total, long linked,
                                 String firstCode, String lastCode, Instant createdAt) {
}
