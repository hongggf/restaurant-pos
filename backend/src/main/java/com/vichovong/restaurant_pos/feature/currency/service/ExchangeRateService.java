package com.vichovong.restaurant_pos.feature.currency.service;

import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateCreateRequest;
import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateResponse;
import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateUpdateRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ExchangeRateService {

    List<ExchangeRateResponse> getAll();

    ExchangeRateResponse create(ExchangeRateCreateRequest request);

    ExchangeRateResponse update(UUID id, ExchangeRateUpdateRequest request);

    BigDecimal convert(BigDecimal amount, String fromCurrencyCode, String toCurrencyCode);
}
