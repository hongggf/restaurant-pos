package com.vichovong.restaurant_pos.feature.table.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.table.entity.DiningTable;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Find-or-create of the one ACTIVE session per table, shared by the guest QR
 * scan and cashier manual session opening (cashier spec §5) so both handle the
 * uq_table_sessions_one_active race identically.
 */
@Component
@RequiredArgsConstructor
public class TableSessionManager {

    private final TableSessionRepository tableSessionRepository;

    // Deliberately not @Transactional: if the insert loses the one-ACTIVE-per-table race,
    // the constraint violation must not doom an enclosing transaction before the re-fetch.
    public TableSession findOrCreateActiveSession(DiningTable table) {
        return tableSessionRepository.findByTableIdAndStatus(table.getId(), SessionStatus.ACTIVE)
                .map(this::touch)
                .orElseGet(() -> {
                    try {
                        TableSession session = new TableSession();
                        session.setTable(table);
                        session.setStatus(SessionStatus.ACTIVE);
                        session.setLastActivityAt(Instant.now());
                        return tableSessionRepository.save(session);
                    } catch (DataIntegrityViolationException e) {
                        // Another phone or the cashier created the session first — share it
                        return tableSessionRepository.findByTableIdAndStatus(table.getId(), SessionStatus.ACTIVE)
                                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                                        "Could not open a session for this table, please retry"));
                    }
                });
    }

    private TableSession touch(TableSession session) {
        session.setLastActivityAt(Instant.now());
        return tableSessionRepository.save(session);
    }
}
