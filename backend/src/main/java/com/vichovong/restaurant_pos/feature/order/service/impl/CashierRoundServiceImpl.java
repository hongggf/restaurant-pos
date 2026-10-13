package com.vichovong.restaurant_pos.feature.order.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.dto.CartLineAddRequest;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionRequest;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineModifierSelection;
import com.vichovong.restaurant_pos.feature.cart.service.CartPricingService;
import com.vichovong.restaurant_pos.feature.cart.service.CartValidationService;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.menu.entity.StationType;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import com.vichovong.restaurant_pos.feature.order.dto.CashierRoundRequest;
import com.vichovong.restaurant_pos.feature.order.dto.CashierRoundResponse;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRoundLineItem;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRoundModifierSelection;
import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;
import com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus;
import com.vichovong.restaurant_pos.feature.order.mapper.OrderRoundMapper;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.order.service.CashierRoundService;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import com.vichovong.restaurant_pos.feature.user.entity.User;
import com.vichovong.restaurant_pos.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashierRoundServiceImpl implements CashierRoundService {

    private final OrderRoundRepository orderRoundRepository;
    private final TableSessionRepository tableSessionRepository;
    private final UserRepository userRepository;
    private final CartValidationService cartValidationService;
    private final CartPricingService cartPricingService;
    private final OrderRoundSnapshotter orderRoundSnapshotter;
    private final OrderRoundMapper orderRoundMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CashierRoundResponse> getQueue(FulfillmentStatus status) {
        return orderRoundRepository.findByFulfillmentStatusOrderBySentAtAsc(status).stream()
                .map(orderRoundMapper::toCashierRoundResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashierRoundResponse> getKitchenQueue(List<com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus> lineStatuses, com.vichovong.restaurant_pos.feature.menu.entity.StationType station) {
        return orderRoundRepository.findByLineStatusesAndStationOrderBySentAtAsc(lineStatuses, station).stream()
                .map(orderRoundMapper::toCashierRoundResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashierRoundResponse> getSessionRounds(UUID sessionId) {
        requireSession(sessionId);
        return orderRoundRepository.findBySessionIdOrderByRoundNumberAsc(sessionId).stream()
                .map(orderRoundMapper::toCashierRoundResponse)
                .toList();
    }

    @Override
    @Transactional
    public CashierRoundResponse startCooking(UUID roundId, com.vichovong.restaurant_pos.feature.menu.entity.StationType station) {
        OrderRound round = requireRound(roundId);
        round.setFulfillmentStatus(FulfillmentStatus.COOKING);
        for (OrderRoundLineItem line : round.getLines()) {
            if (line.getStatus() == com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.SENT && line.getStation() == station) {
                line.setStatus(com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.COOKING);
            }
        }
        return orderRoundMapper.toCashierRoundResponse(round);
    }

    @Override
    @Transactional
    public CashierRoundResponse markReady(UUID roundId, com.vichovong.restaurant_pos.feature.menu.entity.StationType station) {
        OrderRound round = requireRound(roundId);
        round.setFulfillmentStatus(FulfillmentStatus.READY);
        for (OrderRoundLineItem line : round.getLines()) {
            if ((line.getStatus() == com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.SENT || line.getStatus() == com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.COOKING) && line.getStation() == station) {
                line.setStatus(com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.READY);
            }
        }
        return orderRoundMapper.toCashierRoundResponse(round);
    }

    @Override
    @Transactional
    public CashierRoundResponse bump(UUID roundId, com.vichovong.restaurant_pos.feature.menu.entity.StationType station) {
        OrderRound round = requireRound(roundId);
        round.setFulfillmentStatus(FulfillmentStatus.SERVED);
        for (OrderRoundLineItem line : round.getLines()) {
            if ((line.getStatus() == com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.SENT || line.getStatus() == com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.COOKING || line.getStatus() == com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.READY) && line.getStation() == station) {
                line.setStatus(com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.SERVED);
            }
        }
        return orderRoundMapper.toCashierRoundResponse(round);
    }

    @Override
    @Transactional
    public CashierRoundResponse updateLineStatus(UUID roundId, UUID lineId, com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus status) {
        OrderRound round = requireRound(roundId);
        OrderRoundLineItem line = round.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Round line not found: " + lineId));
        line.setStatus(status);
        return orderRoundMapper.toCashierRoundResponse(round);
    }

    @Override
    @Transactional
    public CashierRoundResponse cancel(UUID roundId, String reason) {
        OrderRound round = requireRound(roundId);
        if (round.getFulfillmentStatus() == FulfillmentStatus.COOKING
                || round.getFulfillmentStatus() == FulfillmentStatus.READY
                || round.getFulfillmentStatus() == FulfillmentStatus.SERVED) {
            boolean hasUnservedKitchenLine = round.getLines() != null && round.getLines().stream()
                    .anyMatch(l -> !l.isVoided()
                            && l.getStation() == StationType.KITCHEN
                            && l.getStatus() != LineItemStatus.SERVED);
            if (hasUnservedKitchenLine) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Cannot cancel a round while the kitchen is cooking — bump it instead or wait until it is served");
            }
        }
        if (round.getFulfillmentStatus() != FulfillmentStatus.NEW && round.getFulfillmentStatus() != FulfillmentStatus.READY && round.getFulfillmentStatus() != FulfillmentStatus.COOKING) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only a NEW, COOKING, or READY round can be cancelled (current: " + round.getFulfillmentStatus() + ")");
        }
        round.setFulfillmentStatus(FulfillmentStatus.CANCELLED);
        round.setCancelledAt(Instant.now());
        round.setCancelReason(reason);
        return orderRoundMapper.toCashierRoundResponse(round);
    }

    @Override
    @Transactional
    public CashierRoundResponse voidLine(UUID roundId, UUID lineId, String reason, String username) {
        OrderRound round = requireRound(roundId);
        if (round.getFulfillmentStatus() == FulfillmentStatus.COOKING
                || round.getFulfillmentStatus() == FulfillmentStatus.READY
                || round.getFulfillmentStatus() == FulfillmentStatus.SERVED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot void a line while the kitchen is cooking — bump it instead or wait until it is served");
        }
        if (round.getFulfillmentStatus() != FulfillmentStatus.NEW) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Lines can only be voided on a NEW round (current: " + round.getFulfillmentStatus() + ")");
        }
        OrderRoundLineItem line = round.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Round line not found: " + lineId));
        if (line.isVoided()) {
            throw new ApiException(HttpStatus.CONFLICT, "Line is already voided");
        }

        line.setVoidedAt(Instant.now());
        line.setVoidedBy(userRepository.findByUsername(username).orElse(null));
        line.setVoidReason(reason);
        recomputeTotals(round);
        return orderRoundMapper.toCashierRoundResponse(round);
    }

    @Override
    @Transactional
    public CashierRoundResponse submitRound(UUID sessionId, CashierRoundRequest request) {
        // Same row lock as the guest send: serializes round numbering and close-out
        TableSession session = tableSessionRepository.findByIdForUpdate(sessionId)
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, "Table session is closed"));
        session.setLastActivityAt(Instant.now());

        // Transient cart lines (never persisted) reuse the guest validation and
        // pricing verbatim — no draft cart needed staff-side (cashier spec §5)
        List<CartLineItem> lines = new ArrayList<>(request.lines().size());
        for (CartLineAddRequest lineRequest : request.lines()) {
            MenuItem item = cartValidationService.requireOrderableItem(lineRequest.menuItemId());
            List<ModifierOption> options = cartValidationService.validateSelections(item, lineRequest.selections());

            CartLineItem line = new CartLineItem();
            line.setSession(session);
            line.setMenuItem(item);
            line.setQuantity(lineRequest.quantity());
            line.setRemark(lineRequest.remark());
            for (int i = 0; i < options.size(); i++) {
                CartLineModifierSelection selection = new CartLineModifierSelection();
                selection.setCartLineItem(line);
                selection.setModifierOption(options.get(i));
                selection.setQuantity(lineRequest.selections().get(i).quantity());
                line.getSelections().add(selection);
            }
            lines.add(line);
        }

        CartResponse priced = cartPricingService.price(session, lines);
        // No guest device behind a staff-entered round
        OrderRound round = orderRoundSnapshotter.snapshot(session,
                orderRoundRepository.findMaxRoundNumber(sessionId) + 1, null, lines, priced);

        orderRoundRepository.save(round);
        return orderRoundMapper.toCashierRoundResponse(round);
    }

    @Override
    @Transactional
    public CashierRoundResponse updateLineSelections(UUID roundId, UUID lineId, List<CartSelectionRequest> selections) {
        OrderRound round = requireRound(roundId);
        if (round.getFulfillmentStatus() == FulfillmentStatus.COOKING
                || round.getFulfillmentStatus() == FulfillmentStatus.READY
                || round.getFulfillmentStatus() == FulfillmentStatus.SERVED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot edit a line while the kitchen is cooking — bump it instead or wait until it is served");
        }
        if (round.getFulfillmentStatus() != FulfillmentStatus.NEW) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Selections can only be edited on a NEW round (current: " + round.getFulfillmentStatus() + ")");
        }
        OrderRoundLineItem line = round.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Round line not found: " + lineId));
        if (line.isVoided()) {
            throw new ApiException(HttpStatus.CONFLICT, "Voided lines can't be edited");
        }
        MenuItem item = line.getMenuItem();
        if (item == null) {
            throw new ApiException(HttpStatus.CONFLICT, "This item no longer exists and can't be edited");
        }

        List<ModifierOption> options = cartValidationService.validateSelections(item, selections);

        line.getSelections().clear();
        BigDecimal unitPrice = line.getBasePrice();
        for (int i = 0; i < options.size(); i++) {
            ModifierOption option = options.get(i);
            int quantity = selections.get(i).quantity();

            OrderRoundModifierSelection selection = new OrderRoundModifierSelection();
            selection.setOrderRoundLineItem(line);
            selection.setModifierOption(option);
            selection.setNameEn(option.getNameEn());
            selection.setNameKm(option.getNameKm());
            selection.setUnitPrice(option.getUnitPrice());
            selection.setQuantity(quantity);
            line.getSelections().add(selection);

            unitPrice = unitPrice.add(option.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        line.setUnitPrice(unitPrice);
        line.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(line.getQuantity())).setScale(2, RoundingMode.HALF_UP));

        recomputeTotals(round);
        return orderRoundMapper.toCashierRoundResponse(round);
    }

    /** Bill maths stay consistent: totals always derive from non-voided lines and the round's own vatRate. */
    private void recomputeTotals(OrderRound round) {
        BigDecimal subtotal = round.getLines().stream()
                .filter(l -> !l.isVoided())
                .map(OrderRoundLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = subtotal.multiply(round.getVatRate()).setScale(2, RoundingMode.HALF_UP);
        round.setSubtotal(subtotal);
        round.setVatAmount(vatAmount);
        round.setGrandTotal(subtotal.add(vatAmount));
    }

    private OrderRound requireRound(UUID roundId) {
        return orderRoundRepository.findById(roundId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Round not found: " + roundId));
    }

    private void requireSession(UUID sessionId) {
        if (!tableSessionRepository.existsById(sessionId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId);
        }
    }
}
