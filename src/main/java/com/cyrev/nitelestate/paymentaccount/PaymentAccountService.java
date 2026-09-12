package com.cyrev.nitelestate.paymentaccount;

import com.cyrev.nitelestate.audit.AuditService;
import com.cyrev.nitelestate.common.exception.BadRequestException;
import com.cyrev.nitelestate.common.exception.ConflictException;
import com.cyrev.nitelestate.paymentaccount.dto.ApprovalResponse;
import com.cyrev.nitelestate.paymentaccount.dto.PaymentAccountChangeResponse;
import com.cyrev.nitelestate.paymentaccount.dto.PaymentAccountDecisionRequest;
import com.cyrev.nitelestate.paymentaccount.dto.PaymentAccountRequest;
import com.cyrev.nitelestate.paymentaccount.dto.PaymentAccountResponse;
import com.cyrev.nitelestate.common.exception.NotFoundException;
import com.cyrev.nitelestate.user.Role;
import com.cyrev.nitelestate.user.User;
import com.cyrev.nitelestate.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The estate operates several named payment accounts (e.g. one for Electricity, one for
 * Development) rather than a single shared one - a Levy optionally links to whichever account
 * its payments actually go to (see Levy.paymentAccountId). Every account is under multi-party
 * control: one eligible role proposes creating or editing an account, and it only takes effect
 * once two OTHER eligible roles approve it. A single rejection kills the proposal outright
 * rather than requiring unanimous rejection - a red flag from any financial officer should stop
 * a suspicious change immediately. A second person holding the same role as the proposer, or as
 * an existing approval, doesn't add a new sign-off - this is role-distinctness, not headcount.
 */
@Service
@RequiredArgsConstructor
public class PaymentAccountService {

    /** SUPER_ADMIN included alongside the three named roles per this app's usual convention
     * (always a superset), but it still needs 2 other distinct-role approvals like anyone else -
     * no role can unilaterally change where the estate's money goes. */
    private static final Set<Role> ELIGIBLE_ROLES =
            EnumSet.of(Role.SUPER_ADMIN, Role.CDA_ADMIN, Role.TREASURER, Role.FINANCIAL_SECRETARY);

    private static final int APPROVALS_REQUIRED = 2;

    private final PaymentAccountRepository accountRepository;
    private final PaymentAccountChangeRequestRepository changeRequestRepository;
    private final PaymentAccountChangeApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public List<PaymentAccountResponse> listAccounts() {
        return accountRepository.findAllByActiveTrue().stream().map(PaymentAccountResponse::from).toList();
    }

    public List<PaymentAccountChangeResponse> listPendingChanges() {
        return changeRequestRepository.findAllByStatus(ChangeRequestStatus.PENDING).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public PaymentAccountChangeResponse proposeChange(Long userId, Role role, PaymentAccountRequest request) {
        requireEligible(role);
        Long targetAccountId = request.targetAccountId();

        if (targetAccountId == null) {
            changeRequestRepository.findFirstByTargetAccountIdIsNullAndStatus(ChangeRequestStatus.PENDING)
                    .ifPresent(existing -> {
                        throw new ConflictException("A proposal to create a new account is already pending approval");
                    });
        } else {
            if (!accountRepository.existsById(targetAccountId)) {
                throw new BadRequestException("No payment account found with id " + targetAccountId);
            }
            changeRequestRepository.findFirstByTargetAccountIdAndStatus(targetAccountId, ChangeRequestStatus.PENDING)
                    .ifPresent(existing -> {
                        throw new ConflictException("A change to this account is already pending approval - resolve it first");
                    });
        }

        PaymentAccountChangeRequest change = new PaymentAccountChangeRequest();
        change.setTargetAccountId(targetAccountId);
        change.setLabel(request.label());
        change.setBankName(request.bankName());
        change.setAccountNumber(request.accountNumber());
        change.setAccountName(request.accountName());
        change.setProposedByUserId(userId);
        change.setProposedByRole(role);
        change.setStatus(ChangeRequestStatus.PENDING);
        change = changeRequestRepository.save(change);

        auditService.record("PaymentAccountChangeRequest", change.getId(), "PROPOSE",
                "role=" + role + " target=" + (targetAccountId == null ? "NEW" : targetAccountId)
                        + " label=" + request.label());
        return toResponse(change);
    }

    @Transactional
    public PaymentAccountChangeResponse decide(Long changeId, Long userId, Role role, ApprovalDecision decision,
                                                PaymentAccountDecisionRequest request) {
        requireEligible(role);
        PaymentAccountChangeRequest change = changeRequestRepository.findById(changeId)
                .orElseThrow(() -> NotFoundException.of("PaymentAccountChangeRequest", changeId));
        if (change.getStatus() != ChangeRequestStatus.PENDING) {
            throw new BadRequestException("This change has already been " + change.getStatus().name().toLowerCase());
        }
        if (userId.equals(change.getProposedByUserId())) {
            throw new BadRequestException("You cannot approve or reject your own proposed change");
        }

        List<PaymentAccountChangeApproval> existing = approvalRepository.findByChangeRequestId(changeId);
        boolean sameRoleAsProposer = role == change.getProposedByRole();
        boolean roleAlreadyDecided = existing.stream().anyMatch(a -> a.getRole() == role);
        if (sameRoleAsProposer) {
            throw new BadRequestException(
                    "This change was proposed by a " + roleLabel(role) + " - approval must come from a different role");
        }
        if (roleAlreadyDecided) {
            throw new BadRequestException("A " + roleLabel(role) + " has already responded to this change");
        }

        PaymentAccountChangeApproval approval = new PaymentAccountChangeApproval();
        approval.setChangeRequestId(changeId);
        approval.setUserId(userId);
        approval.setRole(role);
        approval.setDecision(decision);
        approval.setNotes(request.notes());
        approvalRepository.save(approval);
        auditService.record("PaymentAccountChangeRequest", changeId, decision.name(), request.notes());

        if (decision == ApprovalDecision.REJECTED) {
            change.setStatus(ChangeRequestStatus.REJECTED);
            change.setDecidedAt(Instant.now());
            change = changeRequestRepository.save(change);
            return toResponse(change);
        }

        long approvalCount = existing.stream().filter(a -> a.getDecision() == ApprovalDecision.APPROVED).count() + 1;
        if (approvalCount >= APPROVALS_REQUIRED) {
            Long targetAccountId = change.getTargetAccountId();
            PaymentAccount account = targetAccountId != null
                    ? accountRepository.findById(targetAccountId)
                            .orElseThrow(() -> NotFoundException.of("PaymentAccount", targetAccountId))
                    : new PaymentAccount();
            account.setLabel(change.getLabel());
            account.setBankName(change.getBankName());
            account.setAccountNumber(change.getAccountNumber());
            account.setAccountName(change.getAccountName());
            account = accountRepository.save(account);

            change.setStatus(ChangeRequestStatus.APPROVED);
            change.setDecidedAt(Instant.now());
            change = changeRequestRepository.save(change);
            auditService.record("PaymentAccount", account.getId(), "APPLY_CHANGE",
                    "label=" + account.getLabel() + " bank=" + account.getBankName() + " account=" + account.getAccountNumber());
        }
        return toResponse(change);
    }

    private void requireEligible(Role role) {
        if (!ELIGIBLE_ROLES.contains(role)) {
            throw new BadRequestException("Your role cannot propose or approve payment account changes");
        }
    }

    private String roleLabel(Role role) {
        return role.name().replace('_', ' ').toLowerCase();
    }

    private PaymentAccountChangeResponse toResponse(PaymentAccountChangeRequest change) {
        List<PaymentAccountChangeApproval> approvals = approvalRepository.findByChangeRequestId(change.getId());
        List<ApprovalResponse> approvalResponses = approvals.stream()
                .map(a -> ApprovalResponse.from(a, resolveUserName(a.getUserId())))
                .toList();
        long approvedSoFar = approvals.stream().filter(a -> a.getDecision() == ApprovalDecision.APPROVED).count();
        int stillNeeded = change.getStatus() == ChangeRequestStatus.PENDING
                ? (int) Math.max(0, APPROVALS_REQUIRED - approvedSoFar) : 0;
        return PaymentAccountChangeResponse.from(change, resolveUserName(change.getProposedByUserId()),
                approvalResponses, stillNeeded);
    }

    private String resolveUserName(Long userId) {
        return userRepository.findById(userId).map(User::getFullName).orElse(null);
    }
}
