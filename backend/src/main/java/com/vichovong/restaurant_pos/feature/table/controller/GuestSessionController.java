package com.vichovong.restaurant_pos.feature.table.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.feature.table.dto.GuestSessionRequest;
import com.vichovong.restaurant_pos.feature.table.dto.GuestSessionResponse;
import com.vichovong.restaurant_pos.feature.table.service.GuestSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public QR-scan entry point — the only unauthenticated guest endpoint.
 * Returns the same shared session for every phone at the table.
 */
@RestController
@RequestMapping("/api/v1/guest/sessions")
@RequiredArgsConstructor
public class GuestSessionController {

    private final GuestSessionService guestSessionService;

    @PostMapping
    public ResponseEntity<ApiResponse<GuestSessionResponse>> resolve(
            @Valid @RequestBody GuestSessionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(guestSessionService.resolve(request)));
    }
}
