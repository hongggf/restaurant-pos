package com.vichovong.restaurant_pos.feature.report.controller;

import com.vichovong.restaurant_pos.feature.report.dto.OverviewStatsResponse;
import com.vichovong.restaurant_pos.feature.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OverviewStatsResponse>> getOverviewStats() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getOverviewStats()));
    }
}
