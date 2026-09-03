package com.cyrev.nitelestate.security;

import com.cyrev.nitelestate.common.exception.BadRequestException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public CustomUserDetails get() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails details) {
            return details;
        }
        throw new BadRequestException("No authenticated user in context");
    }

    /** Resolves the resident id of the currently authenticated RESIDENT user. */
    public Long residentId() {
        Long residentId = get().getResidentId();
        if (residentId == null) {
            throw new BadRequestException("Current user is not linked to a resident profile");
        }
        return residentId;
    }

    public Long userId() {
        return get().getUserId();
    }
}
