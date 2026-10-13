package com.vichovong.restaurant_pos.feature.order.mapper;

import com.vichovong.restaurant_pos.feature.order.dto.CashierRoundResponse;
import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundLineResponse;
import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundResponse;
import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundSelectionResponse;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRoundLineItem;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRoundModifierSelection;
import org.springframework.stereotype.Component;

/**
 * Hand-written (not MapStruct): responses read the snapshot columns and must
 * tolerate null informational FKs after a menu row is deleted.
 */
@Component
public class OrderRoundMapper {

    public OrderRoundResponse toRoundResponse(OrderRound round) {
        return new OrderRoundResponse(
                round.getId(),
                round.getRoundNumber(),
                round.getPaymentStatus(),
                round.getFulfillmentStatus(),
                round.getSubtotal(),
                round.getVatRate(),
                round.getVatAmount(),
                round.getGrandTotal(),
                round.getSentAt(),
                round.getLines().stream().map(this::toLineResponse).toList()
        );
    }

    public CashierRoundResponse toCashierRoundResponse(OrderRound round) {
        return new CashierRoundResponse(
                round.getId(),
                round.getSession().getId(),
                round.getSession().getTable().getTableNumber(),
                round.getRoundNumber(),
                round.getPaymentStatus(),
                round.getFulfillmentStatus(),
                round.getSubtotal(),
                round.getVatRate(),
                round.getVatAmount(),
                round.getGrandTotal(),
                round.getSentAt(),
                round.getCancelledAt(),
                round.getCancelReason(),
                round.getLines().stream().map(this::toLineResponse).toList()
        );
    }

    public OrderRoundLineResponse toLineResponse(OrderRoundLineItem line) {
        return new OrderRoundLineResponse(
                line.getId(),
                line.getMenuItem() == null ? null : line.getMenuItem().getId(),
                line.getNameEn(),
                line.getNameKm(),
                line.getBasePrice(),
                line.getUnitPrice(),
                line.getQuantity(),
                line.getLineTotal(),
                line.getRemark(),
                line.isVoided(),
                line.getVoidReason(),
                line.getStatus(),
                line.getStation(),
                line.getSelections().stream().map(this::toSelectionResponse).toList()
        );
    }

    private OrderRoundSelectionResponse toSelectionResponse(OrderRoundModifierSelection selection) {
        return new OrderRoundSelectionResponse(
                selection.getModifierOption() == null ? null : selection.getModifierOption().getId(),
                selection.getNameEn(),
                selection.getNameKm(),
                selection.getUnitPrice(),
                selection.getQuantity()
        );
    }
}
