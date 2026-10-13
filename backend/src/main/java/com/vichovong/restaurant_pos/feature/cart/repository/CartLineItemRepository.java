package com.vichovong.restaurant_pos.feature.cart.repository;

import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartLineItemRepository extends JpaRepository<CartLineItem, UUID> {

    List<CartLineItem> findBySessionIdAndDeviceIdOrderByCreatedAtAsc(UUID sessionId, UUID deviceId);

    Optional<CartLineItem> findByIdAndSessionIdAndDeviceId(UUID id, UUID sessionId, UUID deviceId);

    void deleteBySessionIdAndDeviceId(UUID sessionId, UUID deviceId);

    void deleteByMenuItemId(UUID menuItemId);

    boolean existsByMenuItemId(UUID menuItemId);
}
