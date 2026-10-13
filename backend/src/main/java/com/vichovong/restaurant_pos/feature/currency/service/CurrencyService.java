package com.vichovong.restaurant_pos.feature.currency.service;

import com.vichovong.restaurant_pos.feature.currency.dto.CurrencyResponse;

import java.util.List;

public interface CurrencyService {

    List<CurrencyResponse> getAll();
}
