package com.vichovong.restaurant_pos.feature.table.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.table.dto.GuestSessionRequest;
import com.vichovong.restaurant_pos.feature.table.dto.GuestSessionResponse;
import com.vichovong.restaurant_pos.feature.table.entity.DiningTable;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import com.vichovong.restaurant_pos.feature.table.repository.DiningTableRepository;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import com.vichovong.restaurant_pos.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestSessionServiceImplTest {

    @Mock
    private DiningTableRepository diningTableRepository;
    @Mock
    private TableSessionRepository tableSessionRepository;
    @Mock
    private TableSessionManager tableSessionManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private GuestSessionServiceImpl guestSessionService;

    private DiningTable table;
    private TableSession session;

    @BeforeEach
    void setUp() {
        table = new DiningTable();
        table.setId(UUID.randomUUID());
        table.setTableNumber("12");
        table.setQrToken("qr-token-xyz");
        table.setActive(true);

        session = new TableSession();
        session.setId(UUID.randomUUID());
        session.setTable(table);
        session.setStatus(SessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("resolve: resolves valid QR code, ensures session, and mints guest token")
    void resolve_validQrToken_returnsGuestSessionResponse() {
        when(diningTableRepository.findByQrToken("qr-token-xyz")).thenReturn(Optional.of(table));
        when(tableSessionManager.findOrCreateActiveSession(table)).thenReturn(session);
        when(jwtTokenProvider.generateGuestToken(eq(session.getId()), any(UUID.class)))
                .thenReturn("guest-jwt-token");
        when(jwtTokenProvider.getGuestExpirationMs()).thenReturn(14400000L);

        GuestSessionResponse response = guestSessionService.resolve(new GuestSessionRequest("qr-token-xyz"));

        assertThat(response.accessToken()).isEqualTo("guest-jwt-token");
        assertThat(response.expiresInMs()).isEqualTo(14400000L);
        assertThat(response.sessionId()).isEqualTo(session.getId());
        assertThat(response.tableNumber()).isEqualTo("12");
    }

    @Test
    @DisplayName("resolve: throws NOT_FOUND when QR token is invalid or inactive")
    void resolve_invalidQrToken_throwsNotFound() {
        when(diningTableRepository.findByQrToken("unknown-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guestSessionService.resolve(new GuestSessionRequest("unknown-token")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("requireActiveSession: throws GONE when session is closed or missing")
    void requireActiveSession_closedSession_throwsGone() {
        session.setStatus(SessionStatus.CLOSED);
        when(tableSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> guestSessionService.requireActiveSession(session.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.GONE));
    }
}
