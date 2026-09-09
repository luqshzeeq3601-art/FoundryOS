package com.factoryos.modules.reporting.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.reporting.dto.DashboardSummaryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardReportingService dashboardReportingService;

    public DashboardController(DashboardReportingService dashboardReportingService) {
        this.dashboardReportingService = dashboardReportingService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardReportingService.getSummary()));
    }
}
