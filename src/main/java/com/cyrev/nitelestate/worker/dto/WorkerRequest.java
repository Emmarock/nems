package com.cyrev.nitelestate.worker.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * No siteId here on purpose: the site is always the sponsoring resident's own property,
 * resolved server-side in WorkerService.request — a resident cannot request worker access
 * for a property that isn't theirs.
 */
public record WorkerRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        String nationalId,
        @NotBlank String contractorName,
        @NotBlank String workType,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull LocalDate expectedEndDate,
        /** Optional base64 data URI, e.g. "data:image/jpeg;base64,...". Size-capped in WorkerService. */
        String photo
) {
}
