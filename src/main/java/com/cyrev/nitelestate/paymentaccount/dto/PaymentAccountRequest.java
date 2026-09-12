package com.cyrev.nitelestate.paymentaccount.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentAccountRequest(
        @NotBlank String bankName,
        @NotBlank String accountNumber,
        @NotBlank String accountName
) {
}
