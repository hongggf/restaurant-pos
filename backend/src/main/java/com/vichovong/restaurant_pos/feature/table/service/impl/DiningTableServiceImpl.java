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
import com.vichovong.restaurant_pos.feature.table.service.DiningTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiningTableServiceImpl implements DiningTableService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DiningTableRepository diningTableRepository;
    private final TableSessionRepository tableSessionRepository;
    private final TableMapper tableMapper;

    @Override
    public PageResponse<TableResponse> getAll(Pageable pageable) {
        return PageResponse.from(diningTableRepository.findAll(pageable).map(tableMapper::toResponse));
    }

    @Override
    public TableResponse getById(UUID id) {
        return tableMapper.toResponse(requireTable(id));
    }

    @Override
    @Transactional
    public TableResponse create(TableCreateRequest request) {
        if (diningTableRepository.existsByTableNumber(request.tableNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Table number already exists: " + request.tableNumber());
        }
        DiningTable table = new DiningTable();
        table.setTableNumber(request.tableNumber());
        table.setQrToken(generateQrToken());
        return tableMapper.toResponse(diningTableRepository.save(table));
    }

    @Override
    @Transactional
    public TableResponse update(UUID id, TableUpdateRequest request) {
        DiningTable table = requireTable(id);
        if (diningTableRepository.existsByTableNumberAndIdNot(request.tableNumber(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Table number already exists: " + request.tableNumber());
        }
        table.setTableNumber(request.tableNumber());
        table.setActive(request.active());
        return tableMapper.toResponse(diningTableRepository.save(table));
    }

    @Override
    @Transactional
    public TableResponse regenerateQrToken(UUID id) {
        DiningTable table = requireTable(id);
        table.setQrToken(generateQrToken());
        return tableMapper.toResponse(diningTableRepository.save(table));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        DiningTable table = requireTable(id);
        if (tableSessionRepository.existsByTableId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Table has session history and cannot be deleted — deactivate it instead");
        }
        diningTableRepository.delete(table);
    }

    private DiningTable requireTable(UUID id) {
        return diningTableRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Table not found: " + id));
    }

    // 32 random bytes -> 43-char base64url string (256 bits, spec requires >=128)
    private String generateQrToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
