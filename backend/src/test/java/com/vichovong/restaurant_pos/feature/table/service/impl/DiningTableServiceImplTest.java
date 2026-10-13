package com.vichovong.restaurant_pos.feature.table.service.impl;

import com.vichovong.restaurant_pos.common.dto.PageResponse;
import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.table.dto.TableCreateRequest;
import com.vichovong.restaurant_pos.feature.table.dto.TableResponse;
import com.vichovong.restaurant_pos.feature.table.dto.TableUpdateRequest;
import com.vichovong.restaurant_pos.feature.table.entity.DiningTable;
import com.vichovong.restaurant_pos.feature.table.mapper.TableMapper;
import com.vichovong.restaurant_pos.feature.table.repository.DiningTableRepository;
import com.vichovong.restaurant_pos.feature.table.repository.TableSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiningTableServiceImplTest {

    @Mock
    private DiningTableRepository diningTableRepository;
    @Mock
    private TableSessionRepository tableSessionRepository;

    @Spy
    private TableMapper tableMapper = new TableMapper() {
        @Override
        public TableResponse toResponse(DiningTable table) {
            if (table == null) {
                return null;
            }
            return new TableResponse(
                    table.getId(),
                    table.getTableNumber(),
                    table.getQrToken(),
                    table.isActive(),
                    table.getCreatedAt(),
                    table.getUpdatedAt()
            );
        }
    };

    @InjectMocks
    private DiningTableServiceImpl diningTableService;

    private DiningTable table;

    @BeforeEach
    void setUp() {
        table = new DiningTable();
        table.setId(UUID.randomUUID());
        table.setTableNumber("T-01");
        table.setQrToken("initial-qr-token-base64");
        table.setActive(true);
        table.setCreatedAt(Instant.now());
        table.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("getAll: returns paged table responses")
    void getAll_returnsPagedTables() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<DiningTable> page = new PageImpl<>(List.of(table), pageable, 1);
        when(diningTableRepository.findAll(pageable)).thenReturn(page);

        PageResponse<TableResponse> response = diningTableService.getAll(pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().tableNumber()).isEqualTo("T-01");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getById: returns table when found")
    void getById_existingId_returnsTableResponse() {
        when(diningTableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        TableResponse response = diningTableService.getById(table.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(table.getId());
        assertThat(response.tableNumber()).isEqualTo("T-01");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("getById: throws NOT_FOUND when table does not exist")
    void getById_nonExistingId_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(diningTableRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> diningTableService.getById(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("create: generates QR token and saves new dining table")
    void create_validRequest_generatesTokenAndSaves() {
        TableCreateRequest request = new TableCreateRequest("T-05");
        when(diningTableRepository.existsByTableNumber("T-05")).thenReturn(false);
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> {
            DiningTable saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        TableResponse response = diningTableService.create(request);

        assertThat(response.tableNumber()).isEqualTo("T-05");
        assertThat(response.qrToken()).isNotBlank();
        // 32 bytes base64url encoded is 43 characters
        assertThat(response.qrToken().length()).isGreaterThanOrEqualTo(40);
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("create: throws CONFLICT when table number already exists")
    void create_duplicateTableNumber_throwsConflict() {
        TableCreateRequest request = new TableCreateRequest("T-01");
        when(diningTableRepository.existsByTableNumber("T-01")).thenReturn(true);

        assertThatThrownBy(() -> diningTableService.create(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("update: updates table number and toggles active status")
    void update_validRequest_updatesAndReturnsResponse() {
        TableUpdateRequest request = new TableUpdateRequest("T-01-VIP", false);
        when(diningTableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(diningTableRepository.existsByTableNumberAndIdNot("T-01-VIP", table.getId())).thenReturn(false);
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TableResponse response = diningTableService.update(table.getId(), request);

        assertThat(response.tableNumber()).isEqualTo("T-01-VIP");
        assertThat(response.active()).isFalse();
    }

    @Test
    @DisplayName("update: throws CONFLICT when another table has the same number")
    void update_duplicateTableNumberOnOtherTable_throwsConflict() {
        TableUpdateRequest request = new TableUpdateRequest("T-02", true);
        when(diningTableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(diningTableRepository.existsByTableNumberAndIdNot("T-02", table.getId())).thenReturn(true);

        assertThatThrownBy(() -> diningTableService.update(table.getId(), request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("update: throws NOT_FOUND when table does not exist")
    void update_nonExistingTable_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        TableUpdateRequest request = new TableUpdateRequest("T-01", true);
        when(diningTableRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> diningTableService.update(unknownId, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("regenerateQrToken: generates new QR token for table")
    void regenerateQrToken_existingTable_generatesNewToken() {
        String oldToken = table.getQrToken();
        when(diningTableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TableResponse response = diningTableService.regenerateQrToken(table.getId());

        assertThat(response.qrToken()).isNotBlank();
        assertThat(response.qrToken()).isNotEqualTo(oldToken);
    }

    @Test
    @DisplayName("regenerateQrToken: throws NOT_FOUND when table does not exist")
    void regenerateQrToken_nonExistingTable_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(diningTableRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> diningTableService.regenerateQrToken(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("delete: deletes table when no session history exists")
    void delete_noSessionHistory_deletesSuccessfully() {
        when(diningTableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(tableSessionRepository.existsByTableId(table.getId())).thenReturn(false);

        diningTableService.delete(table.getId());

        verify(diningTableRepository).delete(table);
    }

    @Test
    @DisplayName("delete: throws CONFLICT when table has session history")
    void delete_hasSessionHistory_throwsConflict() {
        when(diningTableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(tableSessionRepository.existsByTableId(table.getId())).thenReturn(true);

        assertThatThrownBy(() -> diningTableService.delete(table.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("delete: throws NOT_FOUND when table does not exist")
    void delete_nonExistingTable_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(diningTableRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> diningTableService.delete(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
