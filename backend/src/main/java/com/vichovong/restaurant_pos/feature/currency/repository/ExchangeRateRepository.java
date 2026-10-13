package com.vichovong.restaurant_pos.feature.currency.repository;

import com.vichovong.restaurant_pos.feature.currency.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    Optional<ExchangeRate> findFirstByFromCurrency_CodeAndToCurrency_CodeOrderByEffectiveDateDesc(
            String fromCurrencyCode, String toCurrencyCode);
}
