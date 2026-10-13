package com.vichovong.restaurant_pos.feature.payment.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.currency.service.ExchangeRateService;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.RoundStatus;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.payment.dto.BillResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.PaymentRequest;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;
import com.vichovong.restaurant_pos.feature.payment.entity.Payment;
import com.vichovong.restaurant_pos.feature.payment.entity.PaymentMethod;
import com.vichovong.restaurant_pos.feature.payment.repository.PaymentRepository;
import com.vichovong.restaurant_pos.feature.payment.service.BillingService;
import com.vichovong.restaurant_pos.feature.payment.service.SessionPaymentService;
import com.vichovong.restaurant_pos.feature.receipt.entity.Receipt;
import com.vichovong.restaurant_pos.feature.receipt.service.ReceiptService;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import com.vichovong.restaurant_pos.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.vichovong.restaurant_pos.feature.payment.service.impl.BillingServiceImpl.BASE_CURRENCY;
import static com.vichovong.restaurant_pos.feature.payment.service.impl.BillingServiceImpl.DISPLAY_CURRENCY;

@Service
@RequiredArgsConstructor
public class SessionPaymentServiceImpl implements SessionPaymentService {

    private final TableSessionRepository tableSessionRepository;
    private final OrderRoundRepository orderRoundRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ExchangeRateService exchangeRateService;
    private final BillingService billingService;
    private final ReceiptService receiptService;

    @Override
    @Transactional(readOnly = true)
    public BillResponse getBill(UUID sessionId) {
        TableSession session = tableSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId));
        return billingService.buildBill(session);
    }

    @Override
    @Transactional
    public ReceiptResponse pay(UUID sessionId, PaymentRequest request, String username) {
        // Same row lock as round-send: a guest sending a late round mid-payment
        // waits here, and lands on a CLOSED session (410) instead of a paid bill
        TableSession session = tableSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Session is already closed");
        }

        List<OrderRound> billableRounds = orderRoundRepository
                .findBySessionIdOrderByRoundNumberAsc(sessionId).stream()
                .filter(r -> r.getFulfillmentStatus() != com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.CANCELLED)
                .toList();
        BigDecimal billTotal = billableRounds.stream()
                .map(OrderRound::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (billTotal.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Nothing to pay — the session has no billable rounds");
        }

        Payment payment = buildPayment(session, request, billTotal, username);
        paymentRepository.save(payment);

        // One transaction (cashier spec §6): rounds complete, session closes,
        // the table derives back to IDLE, guest tokens get 410 from now on
        Instant now = Instant.now();
        billableRounds.forEach(round -> round.setPaymentStatus(com.vichovong.restaurant_pos.feature.order.entity.PaymentStatus.PAID));
        session.setStatus(SessionStatus.CLOSED);
        session.setClosedAt(now);

        // Receipt is numbered in the same transaction — no paid session without one
        Receipt receipt = receiptService.createForPayment(payment);
        return billingService.buildReceiptPayload(payment, receipt.getId(), receipt.getReceiptNumber());
    }

    private Payment buildPayment(TableSession session, PaymentRequest request,
                                 BigDecimal billTotal, String username) {
        Payment payment = new Payment();
        payment.setSession(session);
        payment.setMethod(request.method());
        payment.setBillTotal(billTotal);
        payment.setReferenceNote(request.referenceNote());
        payment.setPaidBy(userRepository.findByUsername(username).orElse(null));
        payment.setPaidAt(Instant.now());

        if (request.method() == PaymentMethod.CASH) {
            applyCashTender(payment, request, billTotal);
        } else {
            // QR is verified manually in the cashier's banking app — exact amount, no change
            payment.setAmountTendered(billTotal);
            payment.setTenderedCurrency(BASE_CURRENCY);
            payment.setChangeUsd(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            payment.setChangeKhr(billingService.toKhrOrNull(BigDecimal.ZERO));
        }
        return payment;
    }

    private void applyCashTender(Payment payment, PaymentRequest request, BigDecimal billTotal) {
        if (request.amountTendered() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amountTendered is required for CASH payments");
        }
        String currency = request.currency() == null ? BASE_CURRENCY : request.currency().toUpperCase();
        payment.setAmountTendered(request.amountTendered());
        payment.setTenderedCurrency(currency);

        switch (currency) {
            case "USD" -> {
                requireCovers(request.amountTendered(), billTotal, billTotal, "USD");
                BigDecimal changeUsd = request.amountTendered().subtract(billTotal)
                        .setScale(2, RoundingMode.HALF_UP);
                payment.setChangeUsd(changeUsd);
                payment.setChangeKhr(billingService.toKhrOrNull(changeUsd));
            }
            case "KHR" -> {
                // Tendering KHR requires a configured rate — this propagates if none exists
                BigDecimal billTotalKhr = exchangeRateService.convert(billTotal, BASE_CURRENCY, DISPLAY_CURRENCY);
                requireCovers(request.amountTendered(), billTotalKhr, billTotal, "KHR");
                BigDecimal changeKhr = request.amountTendered().subtract(billTotalKhr)
                        .setScale(2, RoundingMode.HALF_UP);
                payment.setChangeKhr(changeKhr);
                payment.setChangeUsd(exchangeRateService.convert(changeKhr, DISPLAY_CURRENCY, BASE_CURRENCY));
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Unsupported tender currency: " + currency + " (USD or KHR)");
        }
    }

    private void requireCovers(BigDecimal tendered, BigDecimal dueInTenderCurrency,
                               BigDecimal billTotalUsd, String currency) {
        if (tendered.compareTo(dueInTenderCurrency) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Amount tendered (" + tendered + " " + currency + ") is less than the bill total ("
                            + dueInTenderCurrency + " " + currency + " / " + billTotalUsd + " USD)");
        }
    }
}
