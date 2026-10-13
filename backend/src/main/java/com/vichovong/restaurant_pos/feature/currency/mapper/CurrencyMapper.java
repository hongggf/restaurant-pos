package com.vichovong.restaurant_pos.feature.currency.mapper;

import com.vichovong.restaurant_pos.feature.currency.dto.CurrencyResponse;
import com.vichovong.restaurant_pos.feature.currency.entity.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CurrencyMapper {

    default CurrencyResponse toResponse(Currency currency) {
        if (currency == null) {
            return null;
        }
        return new CurrencyResponse(
                currency.getId(),
                currency.getCode(),
                currency.getName(),
                currency.getSymbol(),
                currency.isDefaultCurrency(),
                currency.getCreatedAt(),
                currency.getUpdatedAt()
        );
    }
}
