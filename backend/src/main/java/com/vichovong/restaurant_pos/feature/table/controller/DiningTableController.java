package com.vichovong.restaurant_pos.feature.table.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.common.dto.PageResponse;
import com.vichovong.restaurant_pos.feature.table.dto.StaffSessionResponse;
import com.vichovong.restaurant_pos.feature.table.dto.TableCreateRequest;
import com.vichovong.restaurant_pos.feature.table.dto.TableOverviewResponse;
import com.vichovong.restaurant_pos.feature.table.dto.TableResponse;
import com.vichovong.restaurant_pos.feature.table.dto.TableUpdateRequest;
import com.vichovong.restaurant_pos.feature.table.service.CashierTableService;
import com.vichovong.restaurant_pos.feature.table.service.DiningTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
public class DiningTableController {

    private final DiningTableService diningTableService;
    private final CashierTableService cashierTableService;

    // Polled by the cashier status board every 3-5s (cashier spec §2)
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<List<TableOverviewResponse>>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(cashierTableService.getOverview()));
    }

    @PostMapping("/{id}/session")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<StaffSessionResponse>> openSession(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Session ready",
                cashierTableService.openSession(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<PageResponse<TableResponse>>> getAll(
            @PageableDefault(size = 20, sort = "tableNumber") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(diningTableService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<TableResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(diningTableService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TableResponse>> create(@Valid @RequestBody TableCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Table created", diningTableService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TableResponse>> update(@PathVariable UUID id,
                                                             @Valid @RequestBody TableUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Table updated", diningTableService.update(id, request)));
    }

    @PostMapping("/{id}/qr-token")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TableResponse>> regenerateQrToken(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("QR token regenerated", diningTableService.regenerateQrToken(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        diningTableService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Table deleted", null));
    }
}
