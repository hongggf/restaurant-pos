package com.vichovong.restaurant_pos.feature.receipt.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.payment.dto.BillResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.PaymentResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;
import com.vichovong.restaurant_pos.feature.payment.entity.Payment;
import com.vichovong.restaurant_pos.feature.payment.entity.PaymentMethod;
import com.vichovong.restaurant_pos.feature.payment.service.BillingService;
import com.vichovong.restaurant_pos.feature.receipt.dto.ReceiptPdf;
import com.vichovong.restaurant_pos.feature.receipt.entity.Receipt;
import com.vichovong.restaurant_pos.feature.receipt.repository.ReceiptRepository;
import com.vichovong.restaurant_pos.feature.receipt.service.ReceiptPdfGenerator;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceImplTest {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private BillingService billingService;
    @Mock
    private ReceiptPdfGenerator pdfGenerator;

    @InjectMocks
    private ReceiptServiceImpl receiptService;

    private Payment payment;
    private Receipt receipt;
    private ReceiptResponse receiptResponse;

    @BeforeEach
    void setUp() {
        TableSession session = new TableSession();
        session.setId(UUID.randomUUID());

        payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setSession(session);

        receipt = new Receipt();
        receipt.setId(UUID.randomUUID());
        receipt.setPayment(payment);
        receipt.setReceiptNumber("R-20260823-0042");

        BillResponse billResponse = new BillResponse(
                session.getId(),
                "T-01",
                SessionStatus.CLOSED,
                List.of(),
                "USD",
                new BigDecimal("15.00"),
                new BigDecimal("1.50"),
                new BigDecimal("16.50"),
                new BigDecimal("66000")
        );

        PaymentResponse paymentResponse = new PaymentResponse(
                payment.getId(),
                PaymentMethod.CASH,
                new BigDecimal("16.50"),
                new BigDecimal("20.00"),
                "USD",
                new BigDecimal("3.50"),
                new BigDecimal("14000"),
                null,
                "admin",
                Instant.now()
        );

        receiptResponse = new ReceiptResponse(
                "Malatang Restaurant",
                receipt.getId(),
                receipt.getReceiptNumber(),
                Instant.now(),
                Instant.now(),
                billResponse,
                paymentResponse
        );
    }

    @Test
    @DisplayName("createForPayment: formats receipt number with current date and padded sequence number")
    void createForPayment_generatesCorrectReceiptNumberAndSaves() {
        when(receiptRepository.nextReceiptNumber()).thenReturn(7L);
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Receipt created = receiptService.createForPayment(payment);

        String expectedDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String expectedReceiptNumber = "R-" + expectedDate + "-0007";

        assertThat(created.getPayment()).isEqualTo(payment);
        assertThat(created.getReceiptNumber()).isEqualTo(expectedReceiptNumber);

        ArgumentCaptor<Receipt> captor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptRepository).save(captor.capture());
        assertThat(captor.getValue().getReceiptNumber()).isEqualTo(expectedReceiptNumber);
    }

    @Test
    @DisplayName("get: returns receipt payload when found")
    void get_existingReceipt_returnsReceiptResponse() {
        when(receiptRepository.findById(receipt.getId())).thenReturn(Optional.of(receipt));
        when(billingService.buildReceiptPayload(payment, receipt.getId(), receipt.getReceiptNumber()))
                .thenReturn(receiptResponse);

        ReceiptResponse response = receiptService.get(receipt.getId());

        assertThat(response).isNotNull();
        assertThat(response.receiptNumber()).isEqualTo("R-20260823-0042");
        assertThat(response.restaurantName()).isEqualTo("Malatang Restaurant");
    }

    @Test
    @DisplayName("get: throws NOT_FOUND when receipt does not exist")
    void get_nonExistingReceipt_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(receiptRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiptService.get(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("getBySession: returns receipt for session when found")
    void getBySession_existingSession_returnsReceiptResponse() {
        UUID sessionId = payment.getSession().getId();
        when(receiptRepository.findByPaymentSessionId(sessionId)).thenReturn(Optional.of(receipt));
        when(billingService.buildReceiptPayload(payment, receipt.getId(), receipt.getReceiptNumber()))
                .thenReturn(receiptResponse);

        ReceiptResponse response = receiptService.getBySession(sessionId);

        assertThat(response).isNotNull();
        assertThat(response.receiptNumber()).isEqualTo("R-20260823-0042");
    }

    @Test
    @DisplayName("getBySession: throws NOT_FOUND when session has no receipt")
    void getBySession_noReceiptForSession_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(receiptRepository.findByPaymentSessionId(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiptService.getBySession(sessionId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("pdf: generates PDF document for existing receipt")
    void pdf_existingReceipt_generatesPdf() {
        byte[] samplePdfBytes = "%PDF-1.4 sample content".getBytes();
        when(receiptRepository.findById(receipt.getId())).thenReturn(Optional.of(receipt));
        when(billingService.buildReceiptPayload(payment, receipt.getId(), receipt.getReceiptNumber()))
                .thenReturn(receiptResponse);
        when(pdfGenerator.generate(receiptResponse)).thenReturn(samplePdfBytes);

        ReceiptPdf result = receiptService.pdf(receipt.getId());

        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo("receipt-R-20260823-0042.pdf");
        assertThat(result.content()).isEqualTo(samplePdfBytes);
    }

    @Test
    @DisplayName("pdf: throws NOT_FOUND when receipt does not exist")
    void pdf_nonExistingReceipt_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(receiptRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiptService.pdf(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
