package com.vichovong.restaurant_pos.feature.payment.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.currency.service.ExchangeRateService;
import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundResponse;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.RoundStatus;
import com.vichovong.restaurant_pos.feature.order.mapper.OrderRoundMapper;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.payment.dto.BillResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.PaymentResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;
import com.vichovong.restaurant_pos.feature.payment.entity.Payment;
import com.vichovong.restaurant_pos.feature.payment.service.BillingService;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    // v1: single-restaurant deployment, bills are USD; KHR is the dual-display/tender currency
    static final String BASE_CURRENCY = "USD";
    static final String DISPLAY_CURRENCY = "KHR";

    private final OrderRoundRepository orderRoundRepository;
    private final ExchangeRateService exchangeRateService;
    private final OrderRoundMapper orderRoundMapper;

    // Moves to system settings in Phase 11
    @Value("${app.restaurant.name}")
    private String restaurantName;

    @Override
    public BillResponse buildBill(TableSession session) {
        List<OrderRound> rounds = orderRoundRepository
                .findBySessionIdOrderByRoundNumberAsc(session.getId()).stream()
                .filter(r -> r.getFulfillmentStatus() != com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.CANCELLED)
                .toList();

        BigDecimal subtotal = sum(rounds, OrderRound::getSubtotal);
        BigDecimal vatAmount = sum(rounds, OrderRound::getVatAmount);
        BigDecimal grandTotal = sum(rounds, OrderRound::getGrandTotal);

        return new BillResponse(
                session.getId(),
                session.getTable().getTableNumber(),
                session.getStatus(),
                rounds.stream().map(this::toBillRound).toList(),
                BASE_CURRENCY,
                subtotal,
                vatAmount,
                grandTotal,
                toKhrOrNull(grandTotal)
        );
    }

    @Override
    public ReceiptResponse buildReceiptPayload(Payment payment, UUID receiptId, String receiptNumber) {
        TableSession session = payment.getSession();
        return new ReceiptResponse(
                restaurantName,
                receiptId,
                receiptNumber,
                session.getCreatedAt(),
                session.getClosedAt(),
                buildBill(session),
                toPaymentResponse(payment)
        );
    }

    @Override
    public BigDecimal toKhrOrNull(BigDecimal usdAmount) {
        try {
            return exchangeRateService.convert(usdAmount, BASE_CURRENCY, DISPLAY_CURRENCY);
        } catch (ApiException e) {
            // No configured rate must not break the bill — the client just skips dual display
            return null;
        }
    }

    /** The bill and receipt show non-voided lines only (cashier spec §6). */
    private OrderRoundResponse toBillRound(OrderRound round) {
        OrderRoundResponse full = orderRoundMapper.toRoundResponse(round);
        return new OrderRoundResponse(
                full.id(), full.roundNumber(), full.paymentStatus(), full.fulfillmentStatus(),
                full.subtotal(), full.vatRate(), full.vatAmount(), full.grandTotal(),
                full.sentAt(),
                full.lines().stream().filter(l -> !l.voided()).toList()
        );
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMethod(),
                payment.getBillTotal(),
                payment.getAmountTendered(),
                payment.getTenderedCurrency(),
                payment.getChangeUsd(),
                payment.getChangeKhr(),
                payment.getReferenceNote(),
                payment.getPaidBy() == null ? null : payment.getPaidBy().getUsername(),
                payment.getPaidAt()
        );
    }

    private static BigDecimal sum(List<OrderRound> rounds, Function<OrderRound, BigDecimal> field) {
        return rounds.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
