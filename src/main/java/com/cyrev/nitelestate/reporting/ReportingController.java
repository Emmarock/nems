package com.cyrev.nitelestate.reporting;

import com.cyrev.nitelestate.reporting.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER')")
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return reportingService.dashboard();
    }
}
