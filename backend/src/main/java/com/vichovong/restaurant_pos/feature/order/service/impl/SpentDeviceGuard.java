package com.vichovong.restaurant_pos.feature.order.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * One ordering turn per scan: a device that has sent a round is "spent" and may
 * no longer draft or send — the guest re-scans the table QR for the next round.
 * Reads (menu, orders, running total) stay open to spent devices, so only cart
 * writes and send call this. 403 on a guest endpoint means exactly this state.
 */
@Component
@RequiredArgsConstructor
public class SpentDeviceGuard {

    public static final String MESSAGE = "Order sent — scan the table QR code again to order more";

    private final OrderRoundRepository orderRoundRepository;

    public void requireNotSpent(UUID deviceId) {
        if (orderRoundRepository.existsByDeviceId(deviceId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, MESSAGE);
        }
    }
}
