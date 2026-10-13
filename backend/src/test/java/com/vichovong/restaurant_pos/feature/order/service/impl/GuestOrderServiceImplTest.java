package com.vichovong.restaurant_pos.feature.order.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import com.vichovong.restaurant_pos.feature.cart.repository.CartLineItemRepository;
import com.vichovong.restaurant_pos.feature.cart.service.CartPricingService;
import com.vichovong.restaurant_pos.feature.cart.service.CartValidationService;
import com.vichovong.restaurant_pos.feature.currency.service.ExchangeRateService;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.order.dto.GuestOrdersResponse;
import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundResponse;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;
import com.vichovong.restaurant_pos.feature.order.entity.PaymentStatus;
import com.vichovong.restaurant_pos.feature.order.mapper.OrderRoundMapper;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import com.vichovong.restaurant_pos.feature.table.service.GuestSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestOrderServiceImplTest {

    @Mock
    private OrderRoundRepository orderRoundRepository;
    @Mock
    private CartLineItemRepository cartLineItemRepository;
    @Mock
    private TableSessionRepository tableSessionRepository;
    @Mock
    private GuestSessionService guestSessionService;
    @Mock
    private CartValidationService cartValidationService;
    @Mock
    private CartPricingService cartPricingService;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private OrderRoundSnapshotter orderRoundSnapshotter;
    @Mock
    private OrderRoundMapper orderRoundMapper;
    @Mock
    private SpentDeviceGuard spentDeviceGuard;

    @InjectMocks
    private GuestOrderServiceImpl guestOrderService;

    private TableSession activeSession;
    private UUID deviceId;

    @BeforeEach
    void setUp() {
        activeSession = new TableSession();
        activeSession.setId(UUID.randomUUID());
        activeSession.setStatus(SessionStatus.ACTIVE);

        deviceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("send: throws GONE when table session is closed or missing")
    void send_closedSession_throwsGone() {
        when(tableSessionRepository.findByIdForUpdate(activeSession.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestOrderService.send(activeSession.getId(), deviceId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.GONE));
    }

    @Test
    @DisplayName("send: throws BAD_REQUEST when guest cart is empty")
    void send_emptyCart_throwsBadRequest() {
        when(tableSessionRepository.findByIdForUpdate(activeSession.getId()))
                .thenReturn(Optional.of(activeSession));
        doNothing().when(spentDeviceGuard).requireNotSpent(deviceId);
        when(cartLineItemRepository.findBySessionIdAndDeviceIdOrderByCreatedAtAsc(activeSession.getId(), deviceId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> guestOrderService.send(activeSession.getId(), deviceId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("send: validates, snapshots order round, saves round, and clears cart")
    void send_validCart_snapshotsRoundAndClearsCart() {
        when(tableSessionRepository.findByIdForUpdate(activeSession.getId()))
                .thenReturn(Optional.of(activeSession));
        doNothing().when(spentDeviceGuard).requireNotSpent(deviceId);

        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        item.setPrice(new BigDecimal("5.00"));

        CartLineItem line = new CartLineItem();
        line.setMenuItem(item);
        line.setQuantity(1);
        line.setSelections(new ArrayList<>());

        when(cartLineItemRepository.findBySessionIdAndDeviceIdOrderByCreatedAtAsc(activeSession.getId(), deviceId))
                .thenReturn(List.of(line));
        when(cartValidationService.requireOrderableItem(item.getId())).thenReturn(item);
        when(cartValidationService.validateSelections(eq(item), any())).thenReturn(List.of());

        CartResponse priced = new CartResponse(
                activeSession.getId(), List.of(), "USD",
                new BigDecimal("5.00"), new BigDecimal("0.10"), new BigDecimal("0.50"),
                new BigDecimal("5.50"), null
        );
        when(cartPricingService.price(activeSession, List.of(line))).thenReturn(priced);
        when(orderRoundRepository.findMaxRoundNumber(activeSession.getId())).thenReturn(0);

        OrderRound round = new OrderRound();
        round.setId(UUID.randomUUID());
        round.setRoundNumber(1);
        round.setFulfillmentStatus(FulfillmentStatus.NEW);
        round.setPaymentStatus(PaymentStatus.UNPAID);
        round.setGrandTotal(new BigDecimal("5.50"));
        when(orderRoundSnapshotter.snapshot(activeSession, 1, deviceId, List.of(line), priced))
                .thenReturn(round);

        when(orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(activeSession.getId()))
                .thenReturn(List.of(round));

        OrderRoundResponse roundResp = new OrderRoundResponse(
                round.getId(), 1, PaymentStatus.UNPAID, FulfillmentStatus.NEW,
                new BigDecimal("5.00"), new BigDecimal("0.10"), new BigDecimal("0.50"), new BigDecimal("5.50"),
                Instant.now(), List.of()
        );
        when(orderRoundMapper.toRoundResponse(round)).thenReturn(roundResp);

        GuestOrdersResponse response = guestOrderService.send(activeSession.getId(), deviceId);

        assertThat(response).isNotNull();
        assertThat(response.runningGrandTotal()).isEqualByComparingTo(new BigDecimal("5.50"));
        verify(orderRoundRepository).save(round);
        verify(cartLineItemRepository).deleteBySessionIdAndDeviceId(activeSession.getId(), deviceId);
    }
}
