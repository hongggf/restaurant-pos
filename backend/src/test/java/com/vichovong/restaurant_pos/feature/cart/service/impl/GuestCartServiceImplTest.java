package com.vichovong.restaurant_pos.feature.cart.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.dto.CartLineAddRequest;
import com.vichovong.restaurant_pos.feature.cart.dto.CartLineUpdateRequest;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionRequest;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import com.vichovong.restaurant_pos.feature.cart.repository.CartLineItemRepository;
import com.vichovong.restaurant_pos.feature.cart.service.CartPricingService;
import com.vichovong.restaurant_pos.feature.cart.service.CartValidationService;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import com.vichovong.restaurant_pos.feature.order.service.impl.SpentDeviceGuard;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.service.GuestSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
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
class GuestCartServiceImplTest {

    @Mock
    private CartLineItemRepository cartLineItemRepository;
    @Mock
    private GuestSessionService guestSessionService;
    @Mock
    private CartValidationService cartValidationService;
    @Mock
    private CartPricingService cartPricingService;
    @Mock
    private SpentDeviceGuard spentDeviceGuard;

    @InjectMocks
    private GuestCartServiceImpl guestCartService;

    private TableSession session;
    private UUID deviceId;
    private MenuItem menuItem;
    private CartResponse mockCartResponse;

    @BeforeEach
    void setUp() {
        session = new TableSession();
        session.setId(UUID.randomUUID());

        deviceId = UUID.randomUUID();

        menuItem = new MenuItem();
        menuItem.setId(UUID.randomUUID());
        menuItem.setNameEn("Noodles");
        menuItem.setPrice(new BigDecimal("5.00"));

        mockCartResponse = new CartResponse(
                session.getId(), List.of(), "USD",
                new BigDecimal("5.00"), new BigDecimal("0.10"), new BigDecimal("0.50"),
                new BigDecimal("5.50"), null
        );
    }

    @Test
    @DisplayName("getCart: fetches lines for session and device and prices cart")
    void getCart_validSession_returnsPricedCart() {
        when(guestSessionService.requireActiveSession(session.getId())).thenReturn(session);
        when(cartLineItemRepository.findBySessionIdAndDeviceIdOrderByCreatedAtAsc(session.getId(), deviceId))
                .thenReturn(List.of());
        when(cartPricingService.price(session, List.of())).thenReturn(mockCartResponse);

        CartResponse response = guestCartService.getCart(session.getId(), deviceId);

        assertThat(response).isEqualTo(mockCartResponse);
    }

    @Test
    @DisplayName("addLine: validates item and modifiers, saves line, and returns priced cart")
    void addLine_validItem_savesLineAndReturnsCart() {
        when(guestSessionService.requireActiveSession(session.getId())).thenReturn(session);
        doNothing().when(spentDeviceGuard).requireNotSpent(deviceId);

        ModifierOption option = new ModifierOption();
        option.setId(UUID.randomUUID());
        option.setNameEn("Egg");

        List<CartSelectionRequest> selections = List.of(new CartSelectionRequest(option.getId(), 1));
        CartLineAddRequest request = new CartLineAddRequest(menuItem.getId(), 2, "No onion", selections);

        when(cartValidationService.requireOrderableItem(menuItem.getId())).thenReturn(menuItem);
        when(cartValidationService.validateSelections(menuItem, selections)).thenReturn(List.of(option));
        when(cartPricingService.price(eq(session), any())).thenReturn(mockCartResponse);

        CartResponse response = guestCartService.addLine(session.getId(), deviceId, request);

        assertThat(response).isEqualTo(mockCartResponse);

        ArgumentCaptor<CartLineItem> lineCaptor = ArgumentCaptor.forClass(CartLineItem.class);
        verify(cartLineItemRepository).save(lineCaptor.capture());
        CartLineItem savedLine = lineCaptor.getValue();
        assertThat(savedLine.getSession()).isEqualTo(session);
        assertThat(savedLine.getDeviceId()).isEqualTo(deviceId);
        assertThat(savedLine.getMenuItem()).isEqualTo(menuItem);
        assertThat(savedLine.getQuantity()).isEqualTo(2);
        assertThat(savedLine.getRemark()).isEqualTo("No onion");
        assertThat(savedLine.getSelections()).hasSize(1);
    }

    @Test
    @DisplayName("updateLine: throws NOT_FOUND when line belongs to another session or device")
    void updateLine_lineNotFound_throwsNotFound() {
        UUID lineId = UUID.randomUUID();
        when(guestSessionService.requireActiveSession(session.getId())).thenReturn(session);
        doNothing().when(spentDeviceGuard).requireNotSpent(deviceId);
        when(cartLineItemRepository.findByIdAndSessionIdAndDeviceId(lineId, session.getId(), deviceId))
                .thenReturn(Optional.empty());

        CartLineUpdateRequest request = new CartLineUpdateRequest(3, "Extra spicy", List.of());

        assertThatThrownBy(() -> guestCartService.updateLine(session.getId(), deviceId, lineId, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("removeLine: deletes specified cart line item")
    void removeLine_existingLine_deletesLine() {
        UUID lineId = UUID.randomUUID();
        CartLineItem line = new CartLineItem();
        line.setId(lineId);

        when(guestSessionService.requireActiveSession(session.getId())).thenReturn(session);
        doNothing().when(spentDeviceGuard).requireNotSpent(deviceId);
        when(cartLineItemRepository.findByIdAndSessionIdAndDeviceId(lineId, session.getId(), deviceId))
                .thenReturn(Optional.of(line));
        when(cartPricingService.price(eq(session), any())).thenReturn(mockCartResponse);

        CartResponse response = guestCartService.removeLine(session.getId(), deviceId, lineId);

        assertThat(response).isEqualTo(mockCartResponse);
        verify(cartLineItemRepository).delete(line);
    }

    @Test
    @DisplayName("clear: removes all cart lines for session and device")
    void clear_removesAllLinesForDevice() {
        when(guestSessionService.requireActiveSession(session.getId())).thenReturn(session);
        doNothing().when(spentDeviceGuard).requireNotSpent(deviceId);
        when(cartPricingService.price(eq(session), any())).thenReturn(mockCartResponse);

        CartResponse response = guestCartService.clear(session.getId(), deviceId);

        assertThat(response).isEqualTo(mockCartResponse);
        verify(cartLineItemRepository).deleteBySessionIdAndDeviceId(session.getId(), deviceId);
    }
}
