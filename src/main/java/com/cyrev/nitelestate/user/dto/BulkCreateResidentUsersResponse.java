package com.cyrev.nitelestate.user.dto;

import java.util.List;

public record BulkCreateResidentUsersResponse(
        List<CreatedAccount> created,
        int alreadyHadAccount
) {
    /**
     * The plaintext temporary password is returned here ONCE, at creation time, so an admin can
     * export/print it for handout - it is never stored or retrievable again (only its hash is
     * persisted). Every created account has mustChangePassword=true, so it's useless to anyone
     * but its holder beyond setting a real password.
     */
    public record CreatedAccount(Long residentId, String fullName, String email, String phone, String temporaryPassword) {
    }
}
