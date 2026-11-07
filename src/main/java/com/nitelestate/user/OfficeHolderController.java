package com.nitelestate.user;

import com.nitelestate.security.CurrentUser;
import com.nitelestate.user.dto.OfficeHolderRequest;
import com.nitelestate.user.dto.OfficeHolderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Super admin: which resident holds each estate office - see OfficeHolderService. */
@RestController
@RequestMapping("/api/v1/office-holders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class OfficeHolderController {

    private final OfficeHolderService officeHolderService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<OfficeHolderResponse> list() {
        return officeHolderService.list();
    }

    @PutMapping("/{role}")
    public OfficeHolderResponse assign(@PathVariable Role role, @Valid @RequestBody OfficeHolderRequest request) {
        return officeHolderService.assign(role, request.residentId(), currentUser.userId());
    }

    @DeleteMapping("/{role}")
    public OfficeHolderResponse unassign(@PathVariable Role role) {
        return officeHolderService.unassign(role);
    }
}
