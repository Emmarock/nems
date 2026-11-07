package com.nitelestate.user.dto;

import jakarta.validation.constraints.NotNull;

public record OfficeHolderRequest(@NotNull Long residentId) {
}
