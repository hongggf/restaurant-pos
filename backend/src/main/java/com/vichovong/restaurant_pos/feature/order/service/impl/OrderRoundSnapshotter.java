package com.vichovong.restaurant_pos.feature.order.service.impl;

import com.vichovong.restaurant_pos.feature.cart.dto.CartLineResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionResponse;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRoundLineItem;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRoundModifierSelection;
import com.vichovong.restaurant_pos.feature.order.entity.RoundStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Builds an immutable OrderRound from priced cart lines (spec §B core invariant):
 * names and prices are copied so later menu edits never change the round. Shared
 * by guest cart-send and cashier manual ordering, which prices transient lines
 * through the same CartPricingService.
 */
@Component
public class OrderRoundSnapshotter {

    /**
     * The priced lines are built from {@code lines} in order, so index pairing
     * is safe. The caller persists the returned round (cascade covers children).
     * {@code deviceId} is the sending guest device, or null for cashier rounds.
     */
    public OrderRound snapshot(TableSession session, int roundNumber, UUID deviceId,
                               List<CartLineItem> lines, CartResponse priced) {
        OrderRound round = new OrderRound();
        round.setSession(session);
        round.setRoundNumber(roundNumber);
        round.setDeviceId(deviceId);
        round.setPaymentStatus(com.vichovong.restaurant_pos.feature.order.entity.PaymentStatus.UNPAID);
        round.setFulfillmentStatus(com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.NEW);
        round.setSubtotal(priced.subtotal());
        round.setVatRate(priced.vatRate());
        round.setVatAmount(priced.vatAmount());
        round.setGrandTotal(priced.grandTotal());
        round.setSentAt(Instant.now());

        for (int i = 0; i < lines.size(); i++) {
            CartLineItem cartLine = lines.get(i);
            CartLineResponse pricedLine = priced.lines().get(i);

            OrderRoundLineItem line = new OrderRoundLineItem();
            line.setOrderRound(round);
            line.setMenuItem(cartLine.getMenuItem());
            line.setNameEn(pricedLine.nameEn());
            line.setNameKm(pricedLine.nameKm());
            line.setBasePrice(pricedLine.basePrice());
            line.setUnitPrice(pricedLine.unitPrice());
            line.setQuantity(pricedLine.quantity());
            line.setLineTotal(pricedLine.lineTotal());
            line.setRemark(pricedLine.remark());
            if (cartLine.getMenuItem() != null && cartLine.getMenuItem().getStation() != null) {
                line.setStation(cartLine.getMenuItem().getStation());
            }

            List<CartSelectionResponse> pricedSelections = pricedLine.selections();
            for (int j = 0; j < pricedSelections.size(); j++) {
                CartSelectionResponse pricedSelection = pricedSelections.get(j);

                OrderRoundModifierSelection selection = new OrderRoundModifierSelection();
                selection.setOrderRoundLineItem(line);
                selection.setModifierOption(cartLine.getSelections().get(j).getModifierOption());
                selection.setNameEn(pricedSelection.nameEn());
                selection.setNameKm(pricedSelection.nameKm());
                selection.setUnitPrice(pricedSelection.unitPrice());
                selection.setQuantity(pricedSelection.quantity());
                line.getSelections().add(selection);
            }
            round.getLines().add(line);
        }
        return round;
    }
}
