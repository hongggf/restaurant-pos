package com.vichovong.restaurant_pos.feature.table.service.impl;

import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.OrderRoundLineItem;
import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;
import com.vichovong.restaurant_pos.feature.order.entity.PaymentStatus;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import com.vichovong.restaurant_pos.feature.table.dto.StaffSessionResponse;
import com.vichovong.restaurant_pos.feature.table.dto.TableOverviewResponse;
import com.vichovong.restaurant_pos.feature.table.dto.TableOverviewResponse.TableState;
import com.vichovong.restaurant_pos.feature.table.entity.DiningTable;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.repository.DiningTableRepository;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashierTableServiceImplTest {

    @Mock
    private DiningTableRepository diningTableRepository;
    @Mock
    private TableSessionRepository tableSessionRepository;
    @Mock
    private OrderRoundRepository orderRoundRepository;
    @Mock
    private TableSessionManager tableSessionManager;

    @InjectMocks
    private CashierTableServiceImpl cashierTableService;

    private DiningTable table1;
    private DiningTable table2;
    private DiningTable table3;
    private TableSession session1;
    private TableSession session2;

    @BeforeEach
    void setUp() {
        table1 = new DiningTable();
        table1.setId(UUID.randomUUID());
        table1.setTableNumber("T-01");
        table1.setActive(true);

        table2 = new DiningTable();
        table2.setId(UUID.randomUUID());
        table2.setTableNumber("T-02");
        table2.setActive(true);

        table3 = new DiningTable();
        table3.setId(UUID.randomUUID());
        table3.setTableNumber("T-03");
        table3.setActive(true);

        session1 = new TableSession();
        session1.setId(UUID.randomUUID());
        session1.setTable(table1);
        session1.setStatus(SessionStatus.ACTIVE);

        session2 = new TableSession();
        session2.setId(UUID.randomUUID());
        session2.setTable(table2);
        session2.setStatus(SessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("getOverview: correctly derives IDLE, ORDERED, and SERVED states")
    void getOverview_derivesCorrectTableStates() {
        when(diningTableRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(table1, table2, table3));
        when(tableSessionRepository.findByStatus(SessionStatus.ACTIVE))
                .thenReturn(List.of(session1, session2));

        // Session 1: Has one SENT round -> ORDERED
        OrderRound sentRound = new OrderRound();
        sentRound.setId(UUID.randomUUID());
        sentRound.setSession(session1);
        sentRound.setFulfillmentStatus(FulfillmentStatus.NEW);
        sentRound.setPaymentStatus(PaymentStatus.UNPAID);
        sentRound.setGrandTotal(new BigDecimal("15.00"));
        OrderRoundLineItem sentLine = new OrderRoundLineItem();
        sentLine.setStatus(com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.SENT);
        sentRound.setLines(List.of(sentLine));

        // Session 2: Has one READY round -> SERVED
        OrderRound readyRound = new OrderRound();
        readyRound.setId(UUID.randomUUID());
        readyRound.setSession(session2);
        readyRound.setFulfillmentStatus(FulfillmentStatus.READY);
        readyRound.setPaymentStatus(PaymentStatus.UNPAID);
        readyRound.setGrandTotal(new BigDecimal("25.00"));
        OrderRoundLineItem readyLine = new OrderRoundLineItem();
        readyLine.setStatus(com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus.READY);
        readyRound.setLines(List.of(readyLine));

        when(orderRoundRepository.findBySessionIdIn(anyList()))
                .thenReturn(List.of(sentRound, readyRound));

        List<TableOverviewResponse> overview = cashierTableService.getOverview();

        assertThat(overview).hasSize(3);

        // Table 1 (Session 1): ORDERED
        TableOverviewResponse t1 = overview.stream().filter(t -> t.tableNumber().equals("T-01")).findFirst().orElseThrow();
        assertThat(t1.state()).isEqualTo(TableState.ORDERED);
        assertThat(t1.fulfillmentStatus()).isEqualTo(FulfillmentStatus.NEW);
        assertThat(t1.runningTotal()).isEqualByComparingTo(new BigDecimal("15.00"));

        // Table 2 (Session 2): SERVED
        TableOverviewResponse t2 = overview.stream().filter(t -> t.tableNumber().equals("T-02")).findFirst().orElseThrow();
        assertThat(t2.state()).isEqualTo(TableState.SERVED);
        assertThat(t2.fulfillmentStatus()).isEqualTo(FulfillmentStatus.READY);
        assertThat(t2.runningTotal()).isEqualByComparingTo(new BigDecimal("25.00"));

        // Table 3 (No session): IDLE
        TableOverviewResponse t3 = overview.stream().filter(t -> t.tableNumber().equals("T-03")).findFirst().orElseThrow();
        assertThat(t3.state()).isEqualTo(TableState.IDLE);
        assertThat(t3.fulfillmentStatus()).isNull();
        assertThat(t3.runningTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("openSession: opens or finds active session for walk-in table")
    void openSession_validTable_returnsStaffSessionResponse() {
        when(diningTableRepository.findById(table1.getId())).thenReturn(Optional.of(table1));
        when(tableSessionManager.findOrCreateActiveSession(table1)).thenReturn(session1);
        session1.setCreatedAt(Instant.now());

        StaffSessionResponse response = cashierTableService.openSession(table1.getId());

        assertThat(response.sessionId()).isEqualTo(session1.getId());
        assertThat(response.tableId()).isEqualTo(table1.getId());
        assertThat(response.tableNumber()).isEqualTo("T-01");
        assertThat(response.status()).isEqualTo(SessionStatus.ACTIVE);
    }
}
