package com.cyrev.nitelestate.security;

import com.cyrev.nitelestate.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final Long residentId;
    private final boolean enabled;
    private final boolean mustChangePassword;
    private final String role;
    private final Collection<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.passwordHash = user.getPasswordHash();
        this.residentId = user.getResidentId();
        this.enabled = user.getStatus().name().equals("ACTIVE");
        this.mustChangePassword = user.isMustChangePassword();
        this.role = user.getRole().name();
        // A forced-reset account only gets enough authority to change its own password (that
        // endpoint has no @PreAuthorize, just requires authentication) - every role-gated
        // endpoint's hasAnyRole(...) check naturally denies this authority, no per-endpoint
        // change needed. Re-evaluated fresh from the DB on every request (JwtAuthFilter), so the
        // very next call after changing the password already has the real role - no re-login.
        this.authorities = List.of(new SimpleGrantedAuthority(
                mustChangePassword ? "ROLE_MUST_CHANGE_PASSWORD" : "ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
