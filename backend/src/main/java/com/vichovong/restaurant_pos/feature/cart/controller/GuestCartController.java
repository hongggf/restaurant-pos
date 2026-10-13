package com.vichovong.restaurant_pos.feature.cart.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartLineAddRequest;
import com.vichovong.restaurant_pos.feature.cart.dto.CartLineUpdateRequest;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.service.GuestCartService;
import com.vichovong.restaurant_pos.security.GuestPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Per-device draft cart. Session and device ids always come from the guest
 * token — never from the URL — so no other table or phone can address this
 * draft. Writes return 403 once the device has sent its round (re-scan to
 * order more); reading the (now empty) cart stays allowed.
 */
@RestController
@RequestMapping("/api/v1/guest/cart")
@PreAuthorize("hasRole('GUEST')")
@RequiredArgsConstructor
public class GuestCartController {

    private final GuestCartService guestCartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal GuestPrincipal guest) {
        return ResponseEntity.ok(ApiResponse.success(
                guestCartService.getCart(guest.sessionId(), guest.deviceId())));
    }

    @PostMapping("/lines")
    public ResponseEntity<ApiResponse<CartResponse>> addLine(
            @AuthenticationPrincipal GuestPrincipal guest,
            @Valid @RequestBody CartLineAddRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Line added",
                        guestCartService.addLine(guest.sessionId(), guest.deviceId(), request)));
    }

    @PutMapping("/lines/{lineId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateLine(
            @AuthenticationPrincipal GuestPrincipal guest,
            @PathVariable UUID lineId,
            @Valid @RequestBody CartLineUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Line updated",
                        guestCartService.updateLine(guest.sessionId(), guest.deviceId(), lineId, request)));
    }

    @DeleteMapping("/lines/{lineId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeLine(
            @AuthenticationPrincipal GuestPrincipal guest,
            @PathVariable UUID lineId) {
        return ResponseEntity.ok(
                ApiResponse.success("Line removed",
                        guestCartService.removeLine(guest.sessionId(), guest.deviceId(), lineId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clear(
            @AuthenticationPrincipal GuestPrincipal guest) {
        return ResponseEntity.ok(
                ApiResponse.success("Cart cleared",
                        guestCartService.clear(guest.sessionId(), guest.deviceId())));
    }
}
