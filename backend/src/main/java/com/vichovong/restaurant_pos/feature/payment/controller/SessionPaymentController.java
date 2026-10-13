package com.vichovong.restaurant_pos.feature.payment.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.BillResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.PaymentRequest;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;
import com.vichovong.restaurant_pos.feature.payment.service.SessionPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
@PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
@RequiredArgsConstructor
public class SessionPaymentController {

    private final SessionPaymentService sessionPaymentService;

    @GetMapping("/bill")
    public ResponseEntity<ApiResponse<BillResponse>> getBill(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(sessionPaymentService.getBill(sessionId)));
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<ReceiptResponse>> pay(
            @PathVariable UUID sessionId,
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed",
                sessionPaymentService.pay(sessionId, request, authentication.getName())));
    }
}
