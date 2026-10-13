package com.vichovong.restaurant_pos.feature.currency.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateCreateRequest;
import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateResponse;
import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateUpdateRequest;
import com.vichovong.restaurant_pos.feature.currency.entity.Currency;
import com.vichovong.restaurant_pos.feature.currency.entity.ExchangeRate;
import com.vichovong.restaurant_pos.feature.currency.mapper.ExchangeRateMapper;
import com.vichovong.restaurant_pos.feature.currency.repository.CurrencyRepository;
import com.vichovong.restaurant_pos.feature.currency.repository.ExchangeRateRepository;
import com.vichovong.restaurant_pos.feature.currency.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateMapper exchangeRateMapper;

    @Override
    public List<ExchangeRateResponse> getAll() {
        return exchangeRateRepository.findAll().stream()
                .map(exchangeRateMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExchangeRateResponse create(ExchangeRateCreateRequest request) {
        Currency from = findCurrency(request.fromCurrencyCode());
        Currency to = findCurrency(request.toCurrencyCode());
        if (from.getId().equals(to.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fromCurrencyCode and toCurrencyCode must differ");
        }

        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setFromCurrency(from);
        exchangeRate.setToCurrency(to);
        exchangeRate.setRate(request.rate());
        exchangeRate.setEffectiveDate(request.effectiveDate());

        return exchangeRateMapper.toResponse(exchangeRateRepository.save(exchangeRate));
    }

    @Override
    @Transactional
    public ExchangeRateResponse update(UUID id, ExchangeRateUpdateRequest request) {
        ExchangeRate exchangeRate = exchangeRateRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exchange rate not found: " + id));
        exchangeRate.setRate(request.rate());
        exchangeRate.setEffectiveDate(request.effectiveDate());
        return exchangeRateMapper.toResponse(exchangeRate);
    }

    @Override
    public BigDecimal convert(BigDecimal amount, String fromCurrencyCode, String toCurrencyCode) {
        if (fromCurrencyCode.equalsIgnoreCase(toCurrencyCode)) {
            return amount;
        }
        ExchangeRate exchangeRate = exchangeRateRepository
                .findFirstByFromCurrency_CodeAndToCurrency_CodeOrderByEffectiveDateDesc(fromCurrencyCode, toCurrencyCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No exchange rate found for " + fromCurrencyCode + " -> " + toCurrencyCode));
        return amount.multiply(exchangeRate.getRate()).setScale(2, RoundingMode.HALF_UP);
    }

    private Currency findCurrency(String code) {
        return currencyRepository.findByCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown currency code: " + code));
    }
}
