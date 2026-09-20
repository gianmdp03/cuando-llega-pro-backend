package com.gianmdp03.cuando_llega_pro.domain.dashboard.controller;

import com.gianmdp03.cuando_llega_pro.domain.dashboard.DashboardService;
import com.gianmdp03.cuando_llega_pro.domain.dashboard.dto.DashboardResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * REST controller exposing user-centric aggregated dashboard endpoints.
 */
@RestController
@RequestMapping("/api/v1/me")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Retrieves aggregated real-time dashboard telemetry for all transit presets of the authenticated user.
     *
     * @param principal security principal representing the authenticated user
     * @return 200 OK with consolidated dashboard telemetry
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDTO> getDashboard(Principal principal) {
        DashboardResponseDTO dashboard = dashboardService.getDashboardForUser(principal.getName());
        return ResponseEntity.ok(dashboard);
    }
}
