package com.nitelestate.resident.dto;

/**
 * A landlord adding a tenant also provisions their login in the same step (see
 * ResidentService.createTenant) — the plaintext temporaryPassword is returned here ONCE, same
 * convention as BulkCreateResidentUsersResponse.CreatedAccount, so the landlord can hand it to
 * the tenant directly. mustChangePassword is always true on this account.
 */
public record TenantResponse(
        ResidentResponse resident,
        String loginEmail,
        String loginPhone,
        String temporaryPassword
) {
}
