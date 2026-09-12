package com.cyrev.nitelestate.paymentaccount.dto;

import jakarta.validation.constraints.NotBlank;

/** targetAccountId null proposes creating a brand new account; set, it proposes editing that one. */
public record PaymentAccountRequest(
        Long targetAccountId,
        @NotBlank String label,
        @NotBlank String bankName,
        @NotBlank String accountNumber,
        @NotBlank String accountName
) {
}
