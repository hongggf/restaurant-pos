package com.vichovong.restaurant_pos.feature.order.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionRequest;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import com.vichovong.restaurant_pos.feature.cart.repository.CartLineItemRepository;
import com.vichovong.restaurant_pos.feature.cart.service.CartPricingService;
import com.vichovong.restaurant_pos.feature.cart.service.CartValidationService;
import com.vichovong.restaurant_pos.feature.currency.service.ExchangeRateService;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.order.dto.GuestOrdersResponse;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.RoundStatus;
import com.vichovong.restaurant_pos.feature.order.mapper.OrderRoundMapper;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.order.service.GuestOrderService;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import com.vichovong.restaurant_pos.feature.table.service.GuestSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestOrderServiceImpl implements GuestOrderService {

    // v1: single-restaurant deployment, menu prices are USD; KHR is display-only
    private static final String BASE_CURRENCY = "USD";
    private static final String DISPLAY_CURRENCY = "KHR";

    private final OrderRoundRepository orderRoundRepository;
    private final CartLineItemRepository cartLineItemRepository;
    private final TableSessionRepository tableSessionRepository;
    private final GuestSessionService guestSessionService;
    private final CartValidationService cartValidationService;
    private final CartPricingService cartPricingService;
    private final ExchangeRateService exchangeRateService;
    private final OrderRoundSnapshotter orderRoundSnapshotter;
    private final OrderRoundMapper orderRoundMapper;
    private final SpentDeviceGuard spentDeviceGuard;

    @Override
    @Transactional
    public GuestOrdersResponse send(UUID sessionId, UUID deviceId) {
        // Row lock serializes concurrent sends and close-out for this session (spec §B2);
        // status is re-checked under the lock so a just-closed session can't be sent to.
        TableSession session = tableSessionRepository.findByIdForUpdate(sessionId)
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, "Table session is closed"));
        session.setLastActivityAt(Instant.now());

        // Checked under the lock so a double-tap can't send twice from one device
        spentDeviceGuard.requireNotSpent(deviceId);

        List<CartLineItem> lines =
                cartLineItemRepository.findBySessionIdAndDeviceIdOrderByCreatedAtAsc(sessionId, deviceId);
        if (lines.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        // Re-validate every line against current menu/modifier rows — items or options
        // may have become unavailable since they were added to the cart
        for (CartLineItem line : lines) {
            MenuItem item = cartValidationService.requireOrderableItem(line.getMenuItem().getId());
            cartValidationService.validateSelections(item, toSelectionRequests(line));
        }

        CartResponse priced = cartPricingService.price(session, lines);
        OrderRound round = orderRoundSnapshotter.snapshot(session,
                orderRoundRepository.findMaxRoundNumber(sessionId) + 1, deviceId, lines, priced);

        orderRoundRepository.save(round);
        cartLineItemRepository.deleteBySessionIdAndDeviceId(sessionId, deviceId);

        return buildOrdersResponse(session);
    }

    @Override
    @Transactional
    public GuestOrdersResponse getOrders(UUID sessionId) {
        TableSession session = guestSessionService.requireActiveSession(sessionId);
        return buildOrdersResponse(session);
    }

    private List<CartSelectionRequest> toSelectionRequests(CartLineItem line) {
        return line.getSelections().stream()
                .map(s -> new CartSelectionRequest(s.getModifierOption().getId(), s.getQuantity()))
                .toList();
    }

    private GuestOrdersResponse buildOrdersResponse(TableSession session) {
        List<OrderRound> rounds = orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(session.getId());

        BigDecimal runningGrandTotal = rounds.stream()
                .filter(r -> r.getFulfillmentStatus() != com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.CANCELLED)
                .map(OrderRound::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new GuestOrdersResponse(
                session.getId(),
                rounds.stream().map(orderRoundMapper::toRoundResponse).toList(),
                BASE_CURRENCY,
                runningGrandTotal,
                toKhr(runningGrandTotal)
        );
    }

    private BigDecimal toKhr(BigDecimal usdAmount) {
        try {
            return exchangeRateService.convert(usdAmount, BASE_CURRENCY, DISPLAY_CURRENCY);
        } catch (ApiException e) {
            // No configured rate must not break the orders view — the client just skips dual display
            return null;
        }
    }
}
