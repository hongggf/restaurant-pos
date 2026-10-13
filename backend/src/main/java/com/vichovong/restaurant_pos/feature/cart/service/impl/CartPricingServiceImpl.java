package com.vichovong.restaurant_pos.feature.cart.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.dto.CartLineResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionResponse;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineModifierSelection;
import com.vichovong.restaurant_pos.feature.cart.service.CartPricingService;
import com.vichovong.restaurant_pos.feature.currency.service.ExchangeRateService;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartPricingServiceImpl implements CartPricingService {

    // v1: single-restaurant deployment, menu prices are USD; KHR is display-only
    private static final String BASE_CURRENCY = "USD";
    private static final String DISPLAY_CURRENCY = "KHR";

    private final ExchangeRateService exchangeRateService;

    @Value("${app.tax.rate}")
    private BigDecimal vatRate;

    @Override
    @Transactional(readOnly = true)
    public CartResponse price(TableSession session, List<CartLineItem> lines) {
        List<CartLineResponse> lineResponses = lines.stream().map(this::priceLine).toList();

        BigDecimal subtotal = lineResponses.stream()
                .map(CartLineResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = subtotal.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(vatAmount);

        return new CartResponse(
                session.getId(),
                lineResponses,
                BASE_CURRENCY,
                subtotal,
                vatRate,
                vatAmount,
                grandTotal,
                toKhr(grandTotal)
        );
    }

    private CartLineResponse priceLine(CartLineItem line) {
        MenuItem item = line.getMenuItem();

        BigDecimal unitPrice = item.getPrice();
        List<CartSelectionResponse> selections = line.getSelections().stream()
                .map(this::toSelectionResponse)
                .toList();
        for (CartLineModifierSelection selection : line.getSelections()) {
            unitPrice = unitPrice.add(selection.getModifierOption().getUnitPrice()
                    .multiply(BigDecimal.valueOf(selection.getQuantity())));
        }
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(line.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        return new CartLineResponse(
                line.getId(),
                item.getId(),
                item.getNameEn(),
                item.getNameKm(),
                item.getImageUrl(),
                item.getPrice(),
                line.getQuantity(),
                line.getRemark(),
                selections,
                unitPrice.setScale(2, RoundingMode.HALF_UP),
                lineTotal
        );
    }

    private CartSelectionResponse toSelectionResponse(CartLineModifierSelection selection) {
        ModifierOption option = selection.getModifierOption();
        return new CartSelectionResponse(
                option.getId(),
                option.getNameEn(),
                option.getNameKm(),
                option.getUnitPrice(),
                selection.getQuantity()
        );
    }

    private BigDecimal toKhr(BigDecimal usdAmount) {
        try {
            return exchangeRateService.convert(usdAmount, BASE_CURRENCY, DISPLAY_CURRENCY);
        } catch (ApiException e) {
            // No configured rate must not break the cart — the client just skips dual display
            return null;
        }
    }
}
