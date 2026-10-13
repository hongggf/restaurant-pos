package com.vichovong.restaurant_pos.feature.receipt.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;
import com.vichovong.restaurant_pos.feature.payment.entity.Payment;
import com.vichovong.restaurant_pos.feature.payment.service.BillingService;
import com.vichovong.restaurant_pos.feature.receipt.dto.ReceiptPdf;
import com.vichovong.restaurant_pos.feature.receipt.entity.Receipt;
import com.vichovong.restaurant_pos.feature.receipt.repository.ReceiptRepository;
import com.vichovong.restaurant_pos.feature.receipt.service.ReceiptPdfGenerator;
import com.vichovong.restaurant_pos.feature.receipt.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ReceiptRepository receiptRepository;
    private final BillingService billingService;
    private final ReceiptPdfGenerator pdfGenerator;

    @Override
    @Transactional
    public Receipt createForPayment(Payment payment) {
        Receipt receipt = new Receipt();
        receipt.setPayment(payment);
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        receipt.setReceiptNumber(String.format("R-%s-%04d", datePart, receiptRepository.nextReceiptNumber()));
        return receiptRepository.save(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse get(UUID receiptId) {
        return payload(requireReceipt(receiptId));
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getBySession(UUID sessionId) {
        Receipt receipt = receiptRepository.findByPaymentSessionId(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No receipt for session: " + sessionId));
        return payload(receipt);
    }

    @Override
    @Transactional
    public ReceiptPdf pdf(UUID receiptId) {
        Receipt receipt = requireReceipt(receiptId);
        if (receipt.getFirstPrintedAt() == null) {
            receipt.setFirstPrintedAt(Instant.now());
            receiptRepository.save(receipt);
        }
        return new ReceiptPdf(
                "receipt-" + receipt.getReceiptNumber() + ".pdf",
                pdfGenerator.generate(payload(receipt)));
    }

    private ReceiptResponse payload(Receipt receipt) {
        return billingService.buildReceiptPayload(
                receipt.getPayment(), receipt.getId(), receipt.getReceiptNumber());
    }

    private Receipt requireReceipt(UUID receiptId) {
        return receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Receipt not found: " + receiptId));
    }
}
