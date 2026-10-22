package com.nitelestate.worker.dto;

import com.nitelestate.worker.WorkerCategory;
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
        @NotNull WorkerCategory workerCategory,
        /** Deliberately not @FutureOrPresent - a worker is very often registered days/weeks
         * after they actually started on site (e.g. a landlord only now logging a contractor
         * who's been working since last week), so a past start date is a completely normal,
         * factual entry here, not an error. WorkerService.evaluate's "today.isBefore(startDate)"
         * check already handles a future start date correctly on its own either way. */
        @NotNull LocalDate startDate,
        @NotNull LocalDate expectedEndDate,
        /** Optional base64 data URI, e.g. "data:image/jpeg;base64,...". Size-capped in WorkerService. */
        String photo
) {
}
