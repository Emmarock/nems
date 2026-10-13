package com.nitelestate.accesspolicy.dto;

import com.nitelestate.accesspolicy.AccessPolicySettings;

import java.math.BigDecimal;

public record AccessPolicyResponse(boolean enforceArrears, BigDecimal arrearsThreshold) {
    public static AccessPolicyResponse from(AccessPolicySettings s) {
        return new AccessPolicyResponse(s.isEnforceArrears(), s.getArrearsThreshold());
    }
}
