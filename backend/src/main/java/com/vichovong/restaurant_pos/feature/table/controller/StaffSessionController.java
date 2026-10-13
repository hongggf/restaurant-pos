package com.vichovong.restaurant_pos.feature.table.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.feature.table.dto.TransferSessionRequest;
import com.vichovong.restaurant_pos.feature.table.service.CashierTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class StaffSessionController {

    private final CashierTableService cashierTableService;

    @PatchMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<Void>> transferSession(
            @PathVariable UUID id,
            @Valid @RequestBody TransferSessionRequest request) {
        cashierTableService.transferSession(id, request.targetTableId());
        return ResponseEntity.ok(ApiResponse.success("Session transferred", null));
    }
}
