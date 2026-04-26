package com.jivRas.groceries.controller;

import com.jivRas.groceries.annotation.ModuleAction;
import com.jivRas.groceries.dto.dashboard.MorningSummaryDTO;
import com.jivRas.groceries.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/morning-summary")
    @ModuleAction(module = "DASHBOARD", action = "VIEW")
    public ResponseEntity<MorningSummaryDTO> getMorningSummary() {
        return ResponseEntity.ok(dashboardService.getMorningSummary());
    }
}
