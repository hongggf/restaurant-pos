package com.vichovong.restaurant_pos.feature.currency.mapper;

import com.vichovong.restaurant_pos.feature.currency.dto.ExchangeRateResponse;
import com.vichovong.restaurant_pos.feature.currency.entity.ExchangeRate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExchangeRateMapper {

    default ExchangeRateResponse toResponse(ExchangeRate exchangeRate) {
        if (exchangeRate == null) {
            return null;
        }
        return new ExchangeRateResponse(
                exchangeRate.getId(),
                exchangeRate.getFromCurrency().getCode(),
                exchangeRate.getToCurrency().getCode(),
                exchangeRate.getRate(),
                exchangeRate.getEffectiveDate(),
                exchangeRate.getCreatedAt(),
                exchangeRate.getUpdatedAt()
        );
    }
}
