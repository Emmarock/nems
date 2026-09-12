package com.cyrev.nitelestate.paymentaccount.dto;

import com.cyrev.nitelestate.paymentaccount.ApprovalDecision;
import com.cyrev.nitelestate.paymentaccount.PaymentAccountChangeApproval;
import com.cyrev.nitelestate.user.Role;

import java.time.Instant;

public record ApprovalResponse(
        Long userId,
        String userName,
        Role role,
        ApprovalDecision decision,
        String notes,
        Instant decidedAt
) {
    public static ApprovalResponse from(PaymentAccountChangeApproval a, String userName) {
        return new ApprovalResponse(a.getUserId(), userName, a.getRole(), a.getDecision(), a.getNotes(), a.getCreatedAt());
    }
}
