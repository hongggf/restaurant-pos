package com.vichovong.restaurant_pos.feature.receipt.repository;

import com.vichovong.restaurant_pos.feature.receipt.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    Optional<Receipt> findByPaymentId(UUID paymentId);

    Optional<Receipt> findByPaymentSessionId(UUID sessionId);

    @Query(value = "SELECT nextval('receipt_number_seq')", nativeQuery = true)
    long nextReceiptNumber();
}
