package com.vichovong.restaurant_pos.feature.order.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.feature.order.dto.CashierRoundResponse;
import com.vichovong.restaurant_pos.feature.order.service.CashierRoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The kitchen display's entire API surface: read the cook queue, mark a round
 * ready. It delegates to {@link CashierRoundService}, the same service behind
 * {@code CashierRoundController} — the split is about authorization, not logic.
 *
 * <p>These live on their own paths rather than being added to that controller's
 * class-level {@code @PreAuthorize} because it also exposes cancel, void line
 * and submit round — money-adjacent actions requiring audit reasons. A separate
 * controller keeps the chef's reach explicit, so widening the cashier's
 * permissions later cannot silently widen the kitchen tablet's too.
 */
@RestController
@RequestMapping("/api/v1/kitchen")
@PreAuthorize("hasAnyRole('ADMIN', 'CHEF')")
@RequiredArgsConstructor
public class KitchenController {

    private final CashierRoundService cashierRoundService;

    /** FIFO cook queue, oldest first. Defaults to SENT — what still needs cooking. */
    @GetMapping("/rounds")
    public ResponseEntity<ApiResponse<List<CashierRoundResponse>>> getQueue(
            @RequestParam(defaultValue = "SENT") List<com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus> lineStatus,
            @RequestParam(defaultValue = "KITCHEN") com.vichovong.restaurant_pos.feature.menu.entity.StationType station) {
        return ResponseEntity.ok(ApiResponse.success(cashierRoundService.getKitchenQueue(lineStatus, station)));
    }

    @PutMapping("/rounds/{id}/start-cooking")
    public ResponseEntity<ApiResponse<CashierRoundResponse>> startCooking(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "KITCHEN") com.vichovong.restaurant_pos.feature.menu.entity.StationType station) {
        return ResponseEntity.ok(ApiResponse.success("Round started cooking",
                cashierRoundService.startCooking(id, station)));
    }

    @PutMapping("/rounds/{id}/ready")
    public ResponseEntity<ApiResponse<CashierRoundResponse>> markReady(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "KITCHEN") com.vichovong.restaurant_pos.feature.menu.entity.StationType station) {
        return ResponseEntity.ok(ApiResponse.success("Round marked ready",
                cashierRoundService.markReady(id, station)));
    }

    @PutMapping("/rounds/{id}/bump")
    public ResponseEntity<ApiResponse<CashierRoundResponse>> bump(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "KITCHEN") com.vichovong.restaurant_pos.feature.menu.entity.StationType station) {
        return ResponseEntity.ok(ApiResponse.success("Round bumped",
                cashierRoundService.bump(id, station)));
    }

    @PutMapping("/rounds/{id}/lines/{lineId}/status")
    public ResponseEntity<ApiResponse<CashierRoundResponse>> updateLineStatus(
            @PathVariable UUID id,
            @PathVariable UUID lineId,
            @RequestParam com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Line status updated",
                cashierRoundService.updateLineStatus(id, lineId, status)));
    }
}
