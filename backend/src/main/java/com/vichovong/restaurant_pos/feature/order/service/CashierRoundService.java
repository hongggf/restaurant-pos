package com.vichovong.restaurant_pos.feature.order.service;

import com.vichovong.restaurant_pos.feature.cart.dto.CartSelectionRequest;
import com.vichovong.restaurant_pos.feature.order.dto.CashierRoundRequest;
import com.vichovong.restaurant_pos.feature.order.dto.CashierRoundResponse;
import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;

import java.util.List;
import java.util.UUID;

/**
 * Staff round management (cashier spec §3–§5). Rounds are immutable snapshots:
 * names and base prices from send-time never change. The one deliberate
 * exception is {@link #updateLineSelections}, a cashier-initiated correction
 * that re-snapshots the edited modifiers' current name/price — everything
 * else (void, cancel, a new round) leaves existing lines untouched.
 */
public interface CashierRoundService {

    /** FIFO across all tables, ordered by sentAt — sentAt IS the queue. */
    List<CashierRoundResponse> getQueue(FulfillmentStatus status);

    List<CashierRoundResponse> getKitchenQueue(List<com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus> lineStatuses, com.vichovong.restaurant_pos.feature.menu.entity.StationType station);

    List<CashierRoundResponse> getSessionRounds(UUID sessionId);

    CashierRoundResponse startCooking(UUID roundId, com.vichovong.restaurant_pos.feature.menu.entity.StationType station);

    /** SENT -> READY for a specific station. A future kitchen module takes over this same transition. */
    CashierRoundResponse markReady(UUID roundId, com.vichovong.restaurant_pos.feature.menu.entity.StationType station);

    CashierRoundResponse bump(UUID roundId, com.vichovong.restaurant_pos.feature.menu.entity.StationType station);

    CashierRoundResponse updateLineStatus(UUID roundId, UUID lineId, com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus status);

    /** SENT/READY -> CANCELLED with a required reason; excluded from bill and queue. */
    CashierRoundResponse cancel(UUID roundId, String reason);

    /**
     * Whole-line void with a required reason: sets void fields and recomputes the
     * round's totals from non-voided lines. The row is never deleted.
     */
    CashierRoundResponse voidLine(UUID roundId, UUID lineId, String reason, String username);

    /**
     * Cashier manual ordering: same validation, pricing, and snapshot path as the
     * guest cart-send, but submitted directly as a round in one call.
     */
    CashierRoundResponse submitRound(UUID sessionId, CashierRoundRequest request);

    /**
     * Replaces a SENT/READY line's modifier selections with a newly validated
     * and re-priced set, then recomputes the round's totals. The line's own
     * name/base price snapshot is untouched.
     */
    CashierRoundResponse updateLineSelections(UUID roundId, UUID lineId, List<CartSelectionRequest> selections);
}
