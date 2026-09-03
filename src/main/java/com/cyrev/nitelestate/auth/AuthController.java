package com.cyrev.nitelestate.auth;

import com.cyrev.nitelestate.security.CurrentUser;
import com.cyrev.nitelestate.security.CustomUserDetails;
import com.cyrev.nitelestate.security.JwtService;
import com.cyrev.nitelestate.user.UserService;
import com.cyrev.nitelestate.user.dto.ChangePasswordRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final CurrentUser currentUser;

    @PostMapping("/login")
    @SecurityRequirements
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        return new LoginResponse(token, principal.getUsername(), principal.getFullName(), principal.getRole(),
                principal.getResidentId(), principal.isMustChangePassword());
    }

    /** Self-service, available to every authenticated user regardless of role — proves the current password. */
    @PutMapping("/password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.userId(), request.currentPassword(), request.newPassword());
    }
}
