package com.cyrev.nitelestate.paymentaccount.dto;

import com.cyrev.nitelestate.paymentaccount.PaymentAccountSettings;

public record PaymentAccountResponse(String bankName, String accountNumber, String accountName) {
    public static PaymentAccountResponse from(PaymentAccountSettings s) {
        return new PaymentAccountResponse(s.getBankName(), s.getAccountNumber(), s.getAccountName());
    }
}
