package com.vichovong.restaurant_pos.feature.cart.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.dto.CartLineAddRequest;
import com.vichovong.restaurant_pos.feature.cart.dto.CartLineUpdateRequest;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionRequest;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineModifierSelection;
import com.vichovong.restaurant_pos.feature.cart.repository.CartLineItemRepository;
import com.vichovong.restaurant_pos.feature.cart.service.CartPricingService;
import com.vichovong.restaurant_pos.feature.cart.service.CartValidationService;
import com.vichovong.restaurant_pos.feature.cart.service.GuestCartService;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import com.vichovong.restaurant_pos.feature.order.service.impl.SpentDeviceGuard;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.service.GuestSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Draft carts are private per device (one scan = one device = one ordering turn);
 * writes are blocked once the device has sent its round. Reads stay open.
 */
@Service
@RequiredArgsConstructor
public class GuestCartServiceImpl implements GuestCartService {

    private final CartLineItemRepository cartLineItemRepository;
    private final GuestSessionService guestSessionService;
    private final CartValidationService cartValidationService;
    private final CartPricingService cartPricingService;
    private final SpentDeviceGuard spentDeviceGuard;

    @Override
    @Transactional
    public CartResponse getCart(UUID sessionId, UUID deviceId) {
        TableSession session = guestSessionService.requireActiveSession(sessionId);
        return priceCart(session, deviceId);
    }

    @Override
    @Transactional
    public CartResponse addLine(UUID sessionId, UUID deviceId, CartLineAddRequest request) {
        TableSession session = guestSessionService.requireActiveSession(sessionId);
        spentDeviceGuard.requireNotSpent(deviceId);
        MenuItem item = cartValidationService.requireOrderableItem(request.menuItemId());
        List<ModifierOption> options = cartValidationService.validateSelections(item, request.selections());

        CartLineItem line = new CartLineItem();
        line.setSession(session);
        line.setDeviceId(deviceId);
        line.setMenuItem(item);
        line.setQuantity(request.quantity());
        line.setRemark(request.remark());
        applySelections(line, request.selections(), options);

        cartLineItemRepository.save(line);
        return priceCart(session, deviceId);
    }

    @Override
    @Transactional
    public CartResponse updateLine(UUID sessionId, UUID deviceId, UUID lineId, CartLineUpdateRequest request) {
        TableSession session = guestSessionService.requireActiveSession(sessionId);
        spentDeviceGuard.requireNotSpent(deviceId);
        CartLineItem line = requireLine(lineId, sessionId, deviceId);
        MenuItem item = cartValidationService.requireOrderableItem(line.getMenuItem().getId());
        List<ModifierOption> options = cartValidationService.validateSelections(item, request.selections());

        line.setQuantity(request.quantity());
        line.setRemark(request.remark());
        line.getSelections().clear();
        // Flush orphan deletes before re-inserting: Hibernate orders inserts first,
        // which breaks uq_cart_line_modifier_option when a selection is kept
        cartLineItemRepository.saveAndFlush(line);
        applySelections(line, request.selections(), options);

        cartLineItemRepository.save(line);
        return priceCart(session, deviceId);
    }

    @Override
    @Transactional
    public CartResponse removeLine(UUID sessionId, UUID deviceId, UUID lineId) {
        TableSession session = guestSessionService.requireActiveSession(sessionId);
        spentDeviceGuard.requireNotSpent(deviceId);
        cartLineItemRepository.delete(requireLine(lineId, sessionId, deviceId));
        return priceCart(session, deviceId);
    }

    @Override
    @Transactional
    public CartResponse clear(UUID sessionId, UUID deviceId) {
        TableSession session = guestSessionService.requireActiveSession(sessionId);
        spentDeviceGuard.requireNotSpent(deviceId);
        cartLineItemRepository.deleteBySessionIdAndDeviceId(sessionId, deviceId);
        return priceCart(session, deviceId);
    }

    private CartResponse priceCart(TableSession session, UUID deviceId) {
        return cartPricingService.price(session,
                cartLineItemRepository.findBySessionIdAndDeviceIdOrderByCreatedAtAsc(session.getId(), deviceId));
    }

    private CartLineItem requireLine(UUID lineId, UUID sessionId, UUID deviceId) {
        // Scoped to the token's session and device so no other table — or other
        // phone at the same table — can touch this draft line
        return cartLineItemRepository.findByIdAndSessionIdAndDeviceId(lineId, sessionId, deviceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cart line not found: " + lineId));
    }

    private void applySelections(CartLineItem line, List<CartSelectionRequest> requests,
                                 List<ModifierOption> options) {
        if (requests == null) {
            return;
        }
        for (int i = 0; i < requests.size(); i++) {
            CartLineModifierSelection selection = new CartLineModifierSelection();
            selection.setCartLineItem(line);
            selection.setModifierOption(options.get(i));
            selection.setQuantity(requests.get(i).quantity());
            line.getSelections().add(selection);
        }
    }
}
