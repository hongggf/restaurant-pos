package com.vichovong.restaurant_pos.feature.currency.service.impl;

import com.vichovong.restaurant_pos.feature.currency.dto.CurrencyResponse;
import com.vichovong.restaurant_pos.feature.currency.entity.Currency;
import com.vichovong.restaurant_pos.feature.currency.mapper.CurrencyMapper;
import com.vichovong.restaurant_pos.feature.currency.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceImplTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Spy
    private CurrencyMapper currencyMapper = new CurrencyMapper() {};

    @InjectMocks
    private CurrencyServiceImpl currencyService;

    private Currency usd;
    private Currency khr;

    @BeforeEach
    void setUp() {
        usd = new Currency();
        usd.setId(UUID.randomUUID());
        usd.setCode("USD");
        usd.setName("US Dollar");
        usd.setSymbol("$");
        usd.setDefaultCurrency(true);
        usd.setCreatedAt(Instant.now());
        usd.setUpdatedAt(Instant.now());

        khr = new Currency();
        khr.setId(UUID.randomUUID());
        khr.setCode("KHR");
        khr.setName("Khmer Riel");
        khr.setSymbol("៛");
        khr.setDefaultCurrency(false);
        khr.setCreatedAt(Instant.now());
        khr.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("getAll: returns list of all currencies")
    void getAll_returnsCurrencies() {
        when(currencyRepository.findAll()).thenReturn(List.of(usd, khr));

        List<CurrencyResponse> responses = currencyService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).code()).isEqualTo("USD");
        assertThat(responses.get(0).symbol()).isEqualTo("$");
        assertThat(responses.get(0).defaultCurrency()).isTrue();
        assertThat(responses.get(1).code()).isEqualTo("KHR");
        assertThat(responses.get(1).symbol()).isEqualTo("៛");
        assertThat(responses.get(1).defaultCurrency()).isFalse();
    }
}
