package com.nitelestate.user;

import com.nitelestate.audit.AuditService;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.security.CurrentUser;
import com.nitelestate.user.dto.OfficeProfileResponse;
import com.nitelestate.user.dto.OfficeProfileUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service for staff (every role except RESIDENT): view their office profile and set the
 * personal email that task notifications for their office are sent to.
 */
@RestController
@RequestMapping("/api/v1/account/office-profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated() and !hasRole('RESIDENT')")
public class OfficeProfileController {

    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    @GetMapping
    public OfficeProfileResponse get() {
        return toResponse(me());
    }

    @PutMapping
    @Transactional
    public OfficeProfileResponse update(@Valid @RequestBody OfficeProfileUpdateRequest request) {
        User user = me();
        String email = request.personalEmail() == null || request.personalEmail().isBlank()
                ? null : request.personalEmail().trim();
        user.setPersonalEmail(email);
        user = userRepository.save(user);
        auditService.record("User", user.getId(), "SET_PERSONAL_EMAIL", email == null ? "cleared" : "set");
        return toResponse(user);
    }

    private User me() {
        Long id = currentUser.userId();
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }

    private static OfficeProfileResponse toResponse(User u) {
        return new OfficeProfileResponse(u.getFullName(), u.getEmail(), u.getRole(), u.getPersonalEmail());
    }
}
