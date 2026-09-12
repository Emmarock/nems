package com.cyrev.nitelestate.paymentaccount.dto;

import com.cyrev.nitelestate.paymentaccount.PaymentAccount;

public record PaymentAccountResponse(Long id, String label, String bankName, String accountNumber,
                                      String accountName, boolean active) {
    public static PaymentAccountResponse from(PaymentAccount a) {
        return new PaymentAccountResponse(a.getId(), a.getLabel(), a.getBankName(), a.getAccountNumber(),
                a.getAccountName(), a.isActive());
    }
}
