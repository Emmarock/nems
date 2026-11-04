package com.nitelestate.facematch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A CCTV still (base64 data URI) plus an optional note tying the search to an incident, for the audit log. */
public record FaceSearchRequest(@NotBlank String image, @Size(max = 500) String note) {
}
