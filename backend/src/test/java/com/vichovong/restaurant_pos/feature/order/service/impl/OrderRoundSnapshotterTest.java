package com.vichovong.restaurant_pos.feature.order.service.impl;

import com.vichovong.restaurant_pos.feature.cart.dto.CartLineResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionResponse;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineModifierSelection;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRoundLineItem;
import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;
import com.vichovong.restaurant_pos.feature.order.entity.PaymentStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRoundSnapshotterTest {

    private final OrderRoundSnapshotter snapshotter = new OrderRoundSnapshotter();

    @Test
    @DisplayName("snapshot: creates immutable order round copy with lines and selections")
    void snapshot_validCart_createsImmutableOrderRound() {
        TableSession session = new TableSession();
        session.setId(UUID.randomUUID());

        UUID deviceId = UUID.randomUUID();

        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        item.setNameEn("Mala Tang Soup");
        item.setNameKm("ស៊ុបម៉ាឡា");
        item.setPrice(new BigDecimal("4.00"));

        ModifierOption option = new ModifierOption();
        option.setId(UUID.randomUUID());
        option.setNameEn("Beef Slices");
        option.setNameKm("សាច់គោបន្ទះ");
        option.setUnitPrice(new BigDecimal("1.50"));

        CartLineItem cartLine = new CartLineItem();
        cartLine.setMenuItem(item);
        cartLine.setQuantity(2);
        cartLine.setRemark("Less oil");

        CartLineModifierSelection selection = new CartLineModifierSelection();
        selection.setModifierOption(option);
        selection.setQuantity(1);
        cartLine.setSelections(List.of(selection));

        CartSelectionResponse pricedSelection = new CartSelectionResponse(
                option.getId(), "Beef Slices", "សាច់គោបន្ទះ", new BigDecimal("1.50"), 1
        );

        CartLineResponse pricedLine = new CartLineResponse(
                UUID.randomUUID(), item.getId(), "Mala Tang Soup", "ស៊ុបម៉ាឡា",
                "https://example.com/img.jpg", new BigDecimal("4.00"), 2, "Less oil",
                List.of(pricedSelection), new BigDecimal("5.50"), new BigDecimal("11.00")
        );

        CartResponse priced = new CartResponse(
                session.getId(), List.of(pricedLine), "USD",
                new BigDecimal("11.00"), new BigDecimal("0.10"), new BigDecimal("1.10"),
                new BigDecimal("12.10"), new BigDecimal("48400.00")
        );

        OrderRound round = snapshotter.snapshot(session, 1, deviceId, List.of(cartLine), priced);

        assertThat(round.getSession()).isEqualTo(session);
        assertThat(round.getRoundNumber()).isEqualTo(1);
        assertThat(round.getDeviceId()).isEqualTo(deviceId);
        assertThat(round.getFulfillmentStatus()).isEqualTo(FulfillmentStatus.NEW);
        assertThat(round.getSubtotal()).isEqualByComparingTo(new BigDecimal("11.00"));
        assertThat(round.getVatRate()).isEqualByComparingTo(new BigDecimal("0.10"));
        assertThat(round.getVatAmount()).isEqualByComparingTo(new BigDecimal("1.10"));
        assertThat(round.getGrandTotal()).isEqualByComparingTo(new BigDecimal("12.10"));
        assertThat(round.getSentAt()).isNotNull();

        assertThat(round.getLines()).hasSize(1);
        OrderRoundLineItem roundLine = round.getLines().get(0);
        assertThat(roundLine.getOrderRound()).isEqualTo(round);
        assertThat(roundLine.getMenuItem()).isEqualTo(item);
        assertThat(roundLine.getNameEn()).isEqualTo("Mala Tang Soup");
        assertThat(roundLine.getNameKm()).isEqualTo("ស៊ុបម៉ាឡា");
        assertThat(roundLine.getBasePrice()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(roundLine.getUnitPrice()).isEqualByComparingTo(new BigDecimal("5.50"));
        assertThat(roundLine.getQuantity()).isEqualTo(2);
        assertThat(roundLine.getLineTotal()).isEqualByComparingTo(new BigDecimal("11.00"));
        assertThat(roundLine.getRemark()).isEqualTo("Less oil");

        assertThat(roundLine.getSelections()).hasSize(1);
        assertThat(roundLine.getSelections().get(0).getNameEn()).isEqualTo("Beef Slices");
        assertThat(roundLine.getSelections().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(roundLine.getSelections().get(0).getQuantity()).isEqualTo(1);
    }
}
