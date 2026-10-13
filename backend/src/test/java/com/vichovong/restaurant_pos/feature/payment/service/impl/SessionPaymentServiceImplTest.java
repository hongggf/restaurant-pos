package com.vichovong.restaurant_pos.feature.payment.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.currency.service.ExchangeRateService;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;
import com.vichovong.restaurant_pos.feature.order.entity.PaymentStatus;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.payment.dto.PaymentRequest;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;
import com.vichovong.restaurant_pos.feature.payment.entity.Payment;
import com.vichovong.restaurant_pos.feature.payment.entity.PaymentMethod;
import com.vichovong.restaurant_pos.feature.payment.repository.PaymentRepository;
import com.vichovong.restaurant_pos.feature.payment.service.BillingService;
import com.vichovong.restaurant_pos.feature.receipt.entity.Receipt;
import com.vichovong.restaurant_pos.feature.receipt.service.ReceiptService;
import com.vichovong.restaurant_pos.feature.table.entity.DiningTable;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import com.vichovong.restaurant_pos.feature.user.entity.User;
import com.vichovong.restaurant_pos.feature.user.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionPaymentServiceImplTest {

    @Mock
    private TableSessionRepository tableSessionRepository;
    @Mock
    private OrderRoundRepository orderRoundRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private BillingService billingService;
    @Mock
    private ReceiptService receiptService;

    @InjectMocks
    private SessionPaymentServiceImpl sessionPaymentService;

    private TableSession activeSession;
    private User cashierUser;
    private OrderRound billableRound;

    @BeforeEach
    void setUp() {
        DiningTable table = new DiningTable();
        table.setTableNumber("01");

        activeSession = new TableSession();
        activeSession.setId(UUID.randomUUID());
        activeSession.setStatus(SessionStatus.ACTIVE);
        activeSession.setTable(table);

        cashierUser = new User();
        cashierUser.setId(UUID.randomUUID());
        cashierUser.setUsername("cashier");

        billableRound = new OrderRound();
        billableRound.setId(UUID.randomUUID());
        billableRound.setFulfillmentStatus(FulfillmentStatus.READY);
        billableRound.setPaymentStatus(PaymentStatus.UNPAID);
        billableRound.setGrandTotal(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("pay: throws CONFLICT when session is already closed")
    void pay_alreadyClosedSession_throwsConflict() {
        activeSession.setStatus(SessionStatus.CLOSED);
        when(tableSessionRepository.findByIdForUpdate(activeSession.getId()))
                .thenReturn(Optional.of(activeSession));

        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("20.00"), "USD", null);

        assertThatThrownBy(() -> sessionPaymentService.pay(activeSession.getId(), request, "cashier"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("pay: throws BAD_REQUEST when session has no billable rounds")
    void pay_noBillableRounds_throwsBadRequest() {
        when(tableSessionRepository.findByIdForUpdate(activeSession.getId()))
                .thenReturn(Optional.of(activeSession));
        when(orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(activeSession.getId()))
                .thenReturn(List.of());

        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("20.00"), "USD", null);

        assertThatThrownBy(() -> sessionPaymentService.pay(activeSession.getId(), request, "cashier"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("pay: cash in USD with excess amount calculates correct change and completes payment")
    void pay_cashUsdWithExcessTender_calculatesChangeAndClosesSession() {
        when(tableSessionRepository.findByIdForUpdate(activeSession.getId()))
                .thenReturn(Optional.of(activeSession));
        when(orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(activeSession.getId()))
                .thenReturn(List.of(billableRound));
        when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(cashierUser));

        Receipt receipt = new Receipt();
        receipt.setId(UUID.randomUUID());
        receipt.setReceiptNumber("REC-001");
        when(receiptService.createForPayment(any())).thenReturn(receipt);

        ReceiptResponse mockReceiptResponse = new ReceiptResponse(
                "KaiXin", receipt.getId(), "REC-001", null, null, null, null
        );
        when(billingService.buildReceiptPayload(any(), eq(receipt.getId()), eq("REC-001")))
                .thenReturn(mockReceiptResponse);

        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("50.00"), "USD", "Note");

        ReceiptResponse response = sessionPaymentService.pay(activeSession.getId(), request, "cashier");

        assertThat(response).isNotNull();
        assertThat(activeSession.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(activeSession.getClosedAt()).isNotNull();
        assertThat(billableRound.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getBillTotal()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(saved.getAmountTendered()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(saved.getTenderedCurrency()).isEqualTo("USD");
        assertThat(saved.getChangeUsd()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("pay: cash in KHR calculates KHR change and converts change to USD")
    void pay_cashKhr_calculatesKhrAndUsdChange() {
        when(tableSessionRepository.findByIdForUpdate(activeSession.getId()))
                .thenReturn(Optional.of(activeSession));
        when(orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(activeSession.getId()))
                .thenReturn(List.of(billableRound));
        when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(cashierUser));

        // 20 USD = 80,000 KHR. Tendered 100,000 KHR. Change = 20,000 KHR = 5.00 USD.
        when(exchangeRateService.convert(new BigDecimal("20.00"), "USD", "KHR"))
                .thenReturn(new BigDecimal("80000.00"));
        when(exchangeRateService.convert(new BigDecimal("20000.00"), "KHR", "USD"))
                .thenReturn(new BigDecimal("5.00"));

        Receipt receipt = new Receipt();
        receipt.setId(UUID.randomUUID());
        receipt.setReceiptNumber("REC-002");
        when(receiptService.createForPayment(any())).thenReturn(receipt);

        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("100000.00"), "KHR", null);

        sessionPaymentService.pay(activeSession.getId(), request, "cashier");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getTenderedCurrency()).isEqualTo("KHR");
        assertThat(saved.getChangeKhr()).isEqualByComparingTo(new BigDecimal("20000.00"));
        assertThat(saved.getChangeUsd()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("pay: cash tender less than bill total throws BAD_REQUEST")
    void pay_underpaidCash_throwsBadRequest() {
        when(tableSessionRepository.findByIdForUpdate(activeSession.getId()))
                .thenReturn(Optional.of(activeSession));
        when(orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(activeSession.getId()))
                .thenReturn(List.of(billableRound));

        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, new BigDecimal("15.00"), "USD", null);

        assertThatThrownBy(() -> sessionPaymentService.pay(activeSession.getId(), request, "cashier"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getMessage()).contains("is less than the bill total");
                });
    }

    @Test
    @DisplayName("pay: QR payment settles exact amount with zero change")
    void pay_qrPayment_settlesExactAmount() {
        when(tableSessionRepository.findByIdForUpdate(activeSession.getId()))
                .thenReturn(Optional.of(activeSession));
        when(orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(activeSession.getId()))
                .thenReturn(List.of(billableRound));
        when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(cashierUser));

        Receipt receipt = new Receipt();
        receipt.setId(UUID.randomUUID());
        receipt.setReceiptNumber("REC-003");
        when(receiptService.createForPayment(any())).thenReturn(receipt);

        PaymentRequest request = new PaymentRequest(PaymentMethod.QR, null, null, "QR-Ref-99");

        sessionPaymentService.pay(activeSession.getId(), request, "cashier");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getMethod()).isEqualTo(PaymentMethod.QR);
        assertThat(saved.getAmountTendered()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(saved.getChangeUsd()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
