package com.vichovong.restaurant_pos.feature.table.service.impl;

import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Safety net for abandoned tables: closes ACTIVE sessions with no guest activity
 * for the configured idle window, and cancels any pending cook queue rounds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupJob {

    private final TableSessionRepository tableSessionRepository;
    private final OrderRoundRepository orderRoundRepository;

    @Value("${app.session.idle-timeout:PT4H}")
    private Duration idleTimeout;

    @Scheduled(fixedDelayString = "${app.session.cleanup-interval-ms:900000}")
    @Transactional
    public void closeStaleSessions() {
        Instant cutoff = Instant.now().minus(idleTimeout);
        List<TableSession> stale =
                tableSessionRepository.findByStatusAndLastActivityAtBefore(SessionStatus.ACTIVE, cutoff);
        if (stale.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        List<UUID> staleSessionIds = stale.stream().map(TableSession::getId).toList();
        List<OrderRound> pendingRounds = orderRoundRepository.findBySessionIdIn(staleSessionIds).stream()
                .filter(r -> r.getFulfillmentStatus() == FulfillmentStatus.NEW || r.getFulfillmentStatus() == FulfillmentStatus.READY || r.getFulfillmentStatus() == FulfillmentStatus.COOKING)
                .toList();

        for (OrderRound round : pendingRounds) {
            round.setFulfillmentStatus(FulfillmentStatus.CANCELLED);
            round.setCancelledAt(now);
            round.setCancelReason("Session timed out / abandoned");
        }
        if (!pendingRounds.isEmpty()) {
            orderRoundRepository.saveAll(pendingRounds);
        }

        for (TableSession session : stale) {
            session.setStatus(SessionStatus.CLOSED);
            session.setClosedAt(now);
        }
        tableSessionRepository.saveAll(stale);
        log.info("Auto-closed {} stale table session(s) and cancelled {} pending round(s)",
                stale.size(), pendingRounds.size());
    }
}
