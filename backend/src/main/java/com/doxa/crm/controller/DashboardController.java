package com.doxa.crm.controller;

import com.doxa.crm.dto.dashboard.DailySeriesPoint;
import com.doxa.crm.dto.dashboard.DashboardFilter;
import com.doxa.crm.dto.dashboard.DashboardKpisResponse;
import com.doxa.crm.dto.dashboard.FunnelStepResponse;
import com.doxa.crm.dto.user.TeamUserResponse;
import com.doxa.crm.repository.OpportunityRepository;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.security.RolePolicy;
import com.doxa.crm.service.DashboardService;
import com.doxa.crm.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final TeamService teamService;
    private final OpportunityRepository opportunityRepository;

    @GetMapping("/kpis")
    public DashboardKpisResponse getKpis(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "30") int periodDays,
            @RequestParam(required = false) UUID assignedUserId,
            @RequestParam(required = false) String adSet
    ) {
        DashboardFilter filter = dashboardService.resolveFilter(user, periodDays, assignedUserId, adSet);
        return dashboardService.getKpis(user, filter);
    }

    @GetMapping("/daily-series")
    public List<DailySeriesPoint> getDailySeries(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "30") int periodDays,
            @RequestParam(required = false) UUID assignedUserId,
            @RequestParam(required = false) String adSet
    ) {
        DashboardFilter filter = dashboardService.resolveFilter(user, periodDays, assignedUserId, adSet);
        return dashboardService.getDailySeries(user, filter);
    }

    @GetMapping("/funnel")
    public List<FunnelStepResponse> getFunnel(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "30") int periodDays,
            @RequestParam(required = false) UUID assignedUserId,
            @RequestParam(required = false) String adSet
    ) {
        DashboardFilter filter = dashboardService.resolveFilter(user, periodDays, assignedUserId, adSet);
        return dashboardService.getFunnel(user, filter);
    }

    @GetMapping("/ad-sets")
    public List<String> getAdSets(@AuthenticationPrincipal AuthUser user) {
        return opportunityRepository.findDistinctAdSets(user.getLicenseId());
    }

    @GetMapping("/team")
    public List<TeamUserResponse> getTeam(@AuthenticationPrincipal AuthUser user) {
        RolePolicy.requireAdmin(user);
        return teamService.listTeam(user);
    }
}
