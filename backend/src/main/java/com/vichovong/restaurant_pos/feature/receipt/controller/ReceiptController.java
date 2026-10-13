package com.vichovong.restaurant_pos.feature.receipt.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;
import com.vichovong.restaurant_pos.feature.receipt.dto.ReceiptPdf;
import com.vichovong.restaurant_pos.feature.receipt.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Phase 8 receipts. Receipts are created automatically when a payment confirms;
 * these endpoints serve reprints and the PDF download.
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping("/receipts/{id}")
    public ResponseEntity<ApiResponse<ReceiptResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.get(id)));
    }

    @GetMapping("/sessions/{sessionId}/receipt")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getBySession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.getBySession(sessionId)));
    }

    @GetMapping("/receipts/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        ReceiptPdf pdf = receiptService.pdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(pdf.filename()).build());
        return new ResponseEntity<>(pdf.content(), headers, org.springframework.http.HttpStatus.OK);
    }
}
