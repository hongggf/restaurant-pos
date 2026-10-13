package com.vichovong.restaurant_pos.feature.currency.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateCreateRequest;
import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateResponse;
import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateUpdateRequest;
import com.vichovong.restaurant_pos.feature.currency.service.ExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(exchangeRateService.getAll()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> create(@Valid @RequestBody ExchangeRateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exchange rate created", exchangeRateService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> update(@PathVariable UUID id,
                                                                     @Valid @RequestBody ExchangeRateUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Exchange rate updated", exchangeRateService.update(id, request)));
    }
}
