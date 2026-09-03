package com.cyrev.nitelestate.user;

import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.user.dto.BulkCreateResidentUsersRequest;
import com.cyrev.nitelestate.user.dto.BulkCreateResidentUsersResponse;
import com.cyrev.nitelestate.user.dto.ResetPasswordRequest;
import com.cyrev.nitelestate.user.dto.UserCreateRequest;
import com.cyrev.nitelestate.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administration — Super Admin only manages staff/resident login accounts (spec §10). */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    /** Bulk-creates RESIDENT login accounts (see UserService for the mechanism/rationale). */
    @PostMapping("/bulk-create-residents")
    public BulkCreateResidentUsersResponse bulkCreateResidents(@RequestBody(required = false) BulkCreateResidentUsersRequest request) {
        List<Long> residentIds = request == null ? null : request.residentIds();
        return userService.bulkCreateResidentAccounts(residentIds);
    }

    @GetMapping
    public PageResponse<UserResponse> findAll(@RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return userService.search(q, Paging.of(page, size, Sort.by("fullName")));
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}/status")
    public UserResponse setStatus(@PathVariable Long id, @RequestParam UserStatus status) {
        return userService.setStatus(id, status);
    }

    /** Admin reset (e.g. a resident/staff member forgot their password) — no current-password check. */
    @PutMapping("/{id}/password")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
    }
}
