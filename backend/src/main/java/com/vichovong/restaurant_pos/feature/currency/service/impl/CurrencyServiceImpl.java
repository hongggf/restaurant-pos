package com.vichovong.restaurant_pos.feature.currency.service.impl;

import com.vichovong.restaurant_pos.feature.currency.dto.CurrencyResponse;
import com.vichovong.restaurant_pos.feature.currency.mapper.CurrencyMapper;
import com.vichovong.restaurant_pos.feature.currency.repository.CurrencyRepository;
import com.vichovong.restaurant_pos.feature.currency.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    @Override
    public List<CurrencyResponse> getAll() {
        return currencyRepository.findAll().stream()
                .map(currencyMapper::toResponse)
                .collect(Collectors.toList());
    }
}
