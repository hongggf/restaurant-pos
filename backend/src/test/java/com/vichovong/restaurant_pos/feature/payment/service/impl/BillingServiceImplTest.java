package com.vichovong.restaurant_pos.feature.payment.service.impl;

import com.vichovong.restaurant_pos.feature.currency.service.ExchangeRateService;
import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundLineResponse;
import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundResponse;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;
import com.vichovong.restaurant_pos.feature.order.entity.PaymentStatus;
import com.vichovong.restaurant_pos.feature.order.mapper.OrderRoundMapper;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.payment.dto.BillResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;
import com.vichovong.restaurant_pos.feature.payment.entity.Payment;
import com.vichovong.restaurant_pos.feature.payment.entity.PaymentMethod;
import com.vichovong.restaurant_pos.feature.table.entity.DiningTable;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    @Mock
    private OrderRoundRepository orderRoundRepository;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private OrderRoundMapper orderRoundMapper;

    @InjectMocks
    private BillingServiceImpl billingService;

    private TableSession session;
    private DiningTable table;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(billingService, "restaurantName", "KaiXin Hotpot");

        table = new DiningTable();
        table.setId(UUID.randomUUID());
        table.setTableNumber("T-05");

        session = new TableSession();
        session.setId(UUID.randomUUID());
        session.setTable(table);
        session.setStatus(SessionStatus.ACTIVE);
        session.setCreatedAt(Instant.now().minusSeconds(1800));
    }

    @Test
    @DisplayName("buildBill: sums active rounds and ignores CANCELLED rounds")
    void buildBill_multipleRoundsWithCancelled_sumsOnlyActiveRounds() {
        OrderRound round1 = new OrderRound();
        round1.setId(UUID.randomUUID());
        round1.setRoundNumber(1);
        round1.setFulfillmentStatus(FulfillmentStatus.READY);
        round1.setPaymentStatus(PaymentStatus.UNPAID);
        round1.setSubtotal(new BigDecimal("20.00"));
        round1.setVatAmount(new BigDecimal("2.00"));
        round1.setGrandTotal(new BigDecimal("22.00"));

        OrderRound cancelledRound = new OrderRound();
        cancelledRound.setId(UUID.randomUUID());
        cancelledRound.setRoundNumber(2);
        cancelledRound.setFulfillmentStatus(FulfillmentStatus.CANCELLED);
        cancelledRound.setPaymentStatus(PaymentStatus.UNPAID);
        cancelledRound.setSubtotal(new BigDecimal("15.00"));
        cancelledRound.setVatAmount(new BigDecimal("1.50"));
        cancelledRound.setGrandTotal(new BigDecimal("16.50"));

        OrderRound round3 = new OrderRound();
        round3.setId(UUID.randomUUID());
        round3.setRoundNumber(3);
        round3.setFulfillmentStatus(FulfillmentStatus.NEW);
        round3.setPaymentStatus(PaymentStatus.UNPAID);
        round3.setSubtotal(new BigDecimal("10.00"));
        round3.setVatAmount(new BigDecimal("1.00"));
        round3.setGrandTotal(new BigDecimal("11.00"));

        when(orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(session.getId()))
                .thenReturn(List.of(round1, cancelledRound, round3));

        OrderRoundResponse round1Resp = new OrderRoundResponse(
                round1.getId(), 1, PaymentStatus.UNPAID, FulfillmentStatus.READY,
                new BigDecimal("20.00"), new BigDecimal("0.10"), new BigDecimal("2.00"), new BigDecimal("22.00"),
                Instant.now(), List.of()
        );
        OrderRoundResponse round3Resp = new OrderRoundResponse(
                round3.getId(), 3, PaymentStatus.UNPAID, FulfillmentStatus.NEW,
                new BigDecimal("10.00"), new BigDecimal("0.10"), new BigDecimal("1.00"), new BigDecimal("11.00"),
                Instant.now(), List.of()
        );

        when(orderRoundMapper.toRoundResponse(round1)).thenReturn(round1Resp);
        when(orderRoundMapper.toRoundResponse(round3)).thenReturn(round3Resp);
        when(exchangeRateService.convert(new BigDecimal("33.00"), "USD", "KHR"))
                .thenReturn(new BigDecimal("132000.00"));

        BillResponse bill = billingService.buildBill(session);

        assertThat(bill.sessionId()).isEqualTo(session.getId());
        assertThat(bill.tableNumber()).isEqualTo("T-05");
        assertThat(bill.subtotal()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(bill.vatAmount()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(bill.grandTotal()).isEqualByComparingTo(new BigDecimal("33.00"));
        assertThat(bill.grandTotalKhr()).isEqualByComparingTo(new BigDecimal("132000.00"));
        assertThat(bill.rounds()).hasSize(2);
    }

    @Test
    @DisplayName("buildBill: filters out voided lines from round responses")
    void buildBill_roundWithVoidedLines_excludesVoidedLinesFromResponse() {
        OrderRound round = new OrderRound();
        round.setId(UUID.randomUUID());
        round.setRoundNumber(1);
        round.setFulfillmentStatus(FulfillmentStatus.READY);
        round.setPaymentStatus(PaymentStatus.UNPAID);
        round.setSubtotal(new BigDecimal("10.00"));
        round.setVatAmount(new BigDecimal("1.00"));
        round.setGrandTotal(new BigDecimal("11.00"));

        when(orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(session.getId()))
                .thenReturn(List.of(round));

        OrderRoundLineResponse activeLine = new OrderRoundLineResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Broth", "ទឹកស៊ុប",
                new BigDecimal("10.00"), new BigDecimal("10.00"), 1, new BigDecimal("10.00"),
                "", false, null, com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.SENT, com.vichovong.restaurant_pos.feature.menu.entity.StationType.KITCHEN, List.of()
        );
        OrderRoundLineResponse voidedLine = new OrderRoundLineResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Wrong Dish", "ខុស",
                new BigDecimal("5.00"), new BigDecimal("5.00"), 1, new BigDecimal("5.00"),
                "", true, "Accidental order", com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.SENT, com.vichovong.restaurant_pos.feature.menu.entity.StationType.KITCHEN, List.of()
        );

        OrderRoundResponse roundResp = new OrderRoundResponse(
                round.getId(), 1, PaymentStatus.UNPAID, FulfillmentStatus.READY,
                new BigDecimal("10.00"), new BigDecimal("0.10"), new BigDecimal("1.00"), new BigDecimal("11.00"),
                Instant.now(), List.of(activeLine, voidedLine)
        );
        when(orderRoundMapper.toRoundResponse(round)).thenReturn(roundResp);

        BillResponse bill = billingService.buildBill(session);

        assertThat(bill.rounds()).hasSize(1);
        assertThat(bill.rounds().get(0).lines()).hasSize(1);
        assertThat(bill.rounds().get(0).lines().get(0).nameEn()).isEqualTo("Broth");
    }

    @Test
    @DisplayName("buildReceiptPayload: constructs complete receipt with restaurant name and payment info")
    void buildReceiptPayload_validPayment_returnsReceiptResponse() {
        User cashier = new User();
        cashier.setUsername("cashier01");

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setSession(session);
        payment.setMethod(PaymentMethod.CASH);
        payment.setBillTotal(new BigDecimal("22.00"));
        payment.setAmountTendered(new BigDecimal("30.00"));
        payment.setTenderedCurrency("USD");
        payment.setChangeUsd(new BigDecimal("8.00"));
        payment.setPaidBy(cashier);
        payment.setPaidAt(Instant.now());

        when(orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(session.getId()))
                .thenReturn(List.of());

        UUID receiptId = UUID.randomUUID();
        String receiptNumber = "REC-2026-0001";

        ReceiptResponse response = billingService.buildReceiptPayload(payment, receiptId, receiptNumber);

        assertThat(response.restaurantName()).isEqualTo("KaiXin Hotpot");
        assertThat(response.receiptId()).isEqualTo(receiptId);
        assertThat(response.receiptNumber()).isEqualTo("REC-2026-0001");
        assertThat(response.payment().paidBy()).isEqualTo("cashier01");
        assertThat(response.payment().billTotal()).isEqualByComparingTo(new BigDecimal("22.00"));
        assertThat(response.payment().amountTendered()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(response.payment().changeUsd()).isEqualByComparingTo(new BigDecimal("8.00"));
    }
}
