package com.cyrev.nitelestate.estatesecurity;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SECURITY')")
public class SecurityDashboardController {

    private final SecurityDashboardService securityDashboardService;

    @GetMapping
    public SecurityDashboardResponse dashboard() {
        return securityDashboardService.dashboard();
    }
}
