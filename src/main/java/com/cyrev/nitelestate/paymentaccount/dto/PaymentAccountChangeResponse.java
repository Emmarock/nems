package com.cyrev.nitelestate.paymentaccount.dto;

import com.cyrev.nitelestate.paymentaccount.ChangeRequestStatus;
import com.cyrev.nitelestate.paymentaccount.PaymentAccountChangeRequest;
import com.cyrev.nitelestate.user.Role;

import java.time.Instant;
import java.util.List;

public record PaymentAccountChangeResponse(
        Long id,
        String bankName,
        String accountNumber,
        String accountName,
        Long proposedByUserId,
        String proposedByUserName,
        Role proposedByRole,
        ChangeRequestStatus status,
        List<ApprovalResponse> approvals,
        /** How many more distinct-role approvals are needed before this takes effect (0 once APPROVED/REJECTED). */
        int approvalsStillNeeded,
        Instant createdAt,
        Instant decidedAt
) {
    public static PaymentAccountChangeResponse from(PaymentAccountChangeRequest r, String proposedByUserName,
                                                      List<ApprovalResponse> approvals, int approvalsStillNeeded) {
        return new PaymentAccountChangeResponse(r.getId(), r.getBankName(), r.getAccountNumber(), r.getAccountName(),
                r.getProposedByUserId(), proposedByUserName, r.getProposedByRole(), r.getStatus(), approvals,
                approvalsStillNeeded, r.getCreatedAt(), r.getDecidedAt());
    }
}
