package com.vichovong.restaurant_pos.feature.table.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.RoundStatus;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.table.dto.StaffSessionResponse;
import com.vichovong.restaurant_pos.feature.table.dto.ClosedSessionInfo;
import com.vichovong.restaurant_pos.feature.table.dto.TableOverviewResponse;
import com.vichovong.restaurant_pos.feature.table.dto.TableOverviewResponse.TableState;
import com.vichovong.restaurant_pos.feature.table.entity.DiningTable;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.repository.DiningTableRepository;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import com.vichovong.restaurant_pos.feature.table.service.CashierTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashierTableServiceImpl implements CashierTableService {

    private final DiningTableRepository diningTableRepository;
    private final TableSessionRepository tableSessionRepository;
    private final OrderRoundRepository orderRoundRepository;
    private final TableSessionManager tableSessionManager;

    @Override
    @Transactional(readOnly = true)
    public List<TableOverviewResponse> getOverview() {
        List<DiningTable> tables = diningTableRepository.findAll(Sort.by("tableNumber")).stream()
                .filter(DiningTable::isActive)
                .toList();

        Map<UUID, TableSession> sessionsByTableId = tableSessionRepository
                .findByStatus(SessionStatus.ACTIVE).stream()
                .collect(Collectors.toMap(s -> s.getTable().getId(), Function.identity()));

        Map<UUID, List<OrderRound>> roundsBySessionId = sessionsByTableId.isEmpty()
                ? Map.of()
                : orderRoundRepository
                        .findBySessionIdIn(sessionsByTableId.values().stream().map(TableSession::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(r -> r.getSession().getId()));

        Map<UUID, ClosedSessionInfo> lastClosedByTable = tableSessionRepository.findLatestClosedSessionsInfo().stream()
                .collect(Collectors.toMap(ClosedSessionInfo::tableId, Function.identity(), (a, b) -> a));

        return tables.stream()
                .map(table -> toOverview(table, sessionsByTableId.get(table.getId()), roundsBySessionId, lastClosedByTable.get(table.getId())))
                .toList();
    }

    @Override
    @Transactional
    public void transferSession(UUID sessionId, UUID targetTableId) {
        TableSession session = tableSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));
                
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Can only transfer active sessions");
        }

        DiningTable targetTable = diningTableRepository.findById(targetTableId)
                .filter(DiningTable::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target table not found"));

        if (tableSessionRepository.findByTableIdAndStatus(targetTableId, SessionStatus.ACTIVE).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Target table already has an active session");
        }

        session.setTable(targetTable);
        tableSessionRepository.save(session);
    }

    @Override
    public StaffSessionResponse openSession(UUID tableId) {
        DiningTable table = diningTableRepository.findById(tableId)
                .filter(DiningTable::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Table not found: " + tableId));
        TableSession session = tableSessionManager.findOrCreateActiveSession(table);
        return new StaffSessionResponse(
                session.getId(),
                table.getId(),
                table.getTableNumber(),
                session.getStatus(),
                session.getCreatedAt()
        );
    }

    private TableOverviewResponse toOverview(DiningTable table, TableSession session,
                                             Map<UUID, List<OrderRound>> roundsBySessionId, ClosedSessionInfo lastClosed) {
        if (session == null) {
            return new TableOverviewResponse(table.getId(), table.getTableNumber(),
                    TableState.IDLE, null, null, BigDecimal.ZERO, 
                    lastClosed != null ? lastClosed.sessionId() : null,
                    lastClosed != null ? lastClosed.hasReceipt() : false);
        }

        List<OrderRound> liveRounds = roundsBySessionId
                .getOrDefault(session.getId(), List.of()).stream()
                .filter(r -> r.getFulfillmentStatus() != com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.CANCELLED)
                .toList();

        // Derived, never stored (cashier spec §2): no live rounds -> IDLE;
        // any SENT -> ORDERED; otherwise everything is READY -> SERVED
        boolean hasSent = liveRounds.stream()
                .flatMap(r -> r.getLines().stream())
                .anyMatch(l -> !l.isVoided() && l.getStatus() == com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.SENT);

        TableState state;
        if (liveRounds.isEmpty()) {
            state = TableState.IDLE;
        } else if (hasSent) {
            state = TableState.ORDERED;
        } else {
            state = TableState.SERVED;
        }

        com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus fulfillmentStatus = com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.SERVED;
        if (liveRounds.stream().anyMatch(r -> r.getFulfillmentStatus() == com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.NEW)) {
            fulfillmentStatus = com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.NEW;
        } else if (liveRounds.stream().anyMatch(r -> r.getFulfillmentStatus() == com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.COOKING)) {
            fulfillmentStatus = com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.COOKING;
        } else if (liveRounds.stream().anyMatch(r -> r.getFulfillmentStatus() == com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.READY)) {
            fulfillmentStatus = com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus.READY;
        }

        BigDecimal runningTotal = liveRounds.stream()
                .map(OrderRound::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TableOverviewResponse(table.getId(), table.getTableNumber(),
                state, session.getId(), fulfillmentStatus, runningTotal,
                lastClosed != null ? lastClosed.sessionId() : null,
                lastClosed != null ? lastClosed.hasReceipt() : false);
    }
}
