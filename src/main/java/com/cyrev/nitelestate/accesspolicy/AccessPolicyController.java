package com.cyrev.nitelestate.accesspolicy;

import com.cyrev.nitelestate.accesspolicy.dto.AccessPolicyRequest;
import com.cyrev.nitelestate.accesspolicy.dto.AccessPolicyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/access-policy")
@RequiredArgsConstructor
public class AccessPolicyController {

    private final AccessPolicyService accessPolicyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
    public AccessPolicyResponse get() {
        return accessPolicyService.getSettings();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
    public AccessPolicyResponse update(@Valid @RequestBody AccessPolicyRequest request) {
        return accessPolicyService.updateSettings(request);
    }
}
