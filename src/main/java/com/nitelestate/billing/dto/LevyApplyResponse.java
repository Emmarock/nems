package com.nitelestate.billing.dto;

/** Result of applying a levy to every active resident - see LevyService.applyToAllResidents. */
public record LevyApplyResponse(int residentsProcessed) {
}
