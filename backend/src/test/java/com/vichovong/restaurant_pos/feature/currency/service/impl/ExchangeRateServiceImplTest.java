package com.vichovong.restaurant_pos.feature.currency.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateCreateRequest;
import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateResponse;
import com.vichovong.restaurant_pos.feature.currency.entity.Currency;
import com.vichovong.restaurant_pos.feature.currency.entity.ExchangeRate;
import com.vichovong.restaurant_pos.feature.currency.mapper.ExchangeRateMapper;
import com.vichovong.restaurant_pos.feature.currency.repository.CurrencyRepository;
import com.vichovong.restaurant_pos.feature.currency.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceImplTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    private Currency usd;
    private Currency khr;

    @BeforeEach
    void setUp() {
        usd = new Currency();
        usd.setId(UUID.randomUUID());
        usd.setCode("USD");
        usd.setName("US Dollar");

        khr = new Currency();
        khr.setId(UUID.randomUUID());
        khr.setCode("KHR");
        khr.setName("Cambodian Riel");
    }

    @Test
    @DisplayName("convert: same currency returns amount unchanged")
    void convert_sameCurrency_returnsAmountUnchanged() {
        BigDecimal amount = new BigDecimal("25.50");
        BigDecimal result = exchangeRateService.convert(amount, "USD", "USD");
        assertThat(result).isEqualTo(amount);
    }

    @Test
    @DisplayName("convert: multiplies amount by rate with scale 2 HALF_UP")
    void convert_usdToKhr_multipliesByRate() {
        ExchangeRate rate = new ExchangeRate();
        rate.setFromCurrency(usd);
        rate.setToCurrency(khr);
        rate.setRate(new BigDecimal("4100.00"));
        rate.setEffectiveDate(LocalDate.now());

        when(exchangeRateRepository.findFirstByFromCurrency_CodeAndToCurrency_CodeOrderByEffectiveDateDesc("USD", "KHR"))
                .thenReturn(Optional.of(rate));

        BigDecimal result = exchangeRateService.convert(new BigDecimal("10.00"), "USD", "KHR");
        assertThat(result).isEqualByComparingTo(new BigDecimal("41000.00"));
    }

    @Test
    @DisplayName("convert: throws NOT_FOUND when no exchange rate exists")
    void convert_rateNotFound_throwsNotFound() {
        when(exchangeRateRepository.findFirstByFromCurrency_CodeAndToCurrency_CodeOrderByEffectiveDateDesc("USD", "EUR"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeRateService.convert(new BigDecimal("10.00"), "USD", "EUR"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("create: throws BAD_REQUEST when from and to currencies are identical")
    void create_sameCurrencies_throwsBadRequest() {
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usd));

        ExchangeRateCreateRequest request = new ExchangeRateCreateRequest(
                "USD", "USD", new BigDecimal("1.00"), LocalDate.now()
        );

        assertThatThrownBy(() -> exchangeRateService.create(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
