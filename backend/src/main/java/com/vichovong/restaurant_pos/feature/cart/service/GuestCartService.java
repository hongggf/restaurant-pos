package com.vichovong.restaurant_pos.feature.cart.service;

import com.vichovong.restaurant_pos.feature.cart.dto.CartLineAddRequest;
import com.vichovong.restaurant_pos.feature.cart.dto.CartLineUpdateRequest;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;

import java.util.UUID;

public interface GuestCartService {

    CartResponse getCart(UUID sessionId, UUID deviceId);

    CartResponse addLine(UUID sessionId, UUID deviceId, CartLineAddRequest request);

    CartResponse updateLine(UUID sessionId, UUID deviceId, UUID lineId, CartLineUpdateRequest request);

    CartResponse removeLine(UUID sessionId, UUID deviceId, UUID lineId);

    CartResponse clear(UUID sessionId, UUID deviceId);
}
