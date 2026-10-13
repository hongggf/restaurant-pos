package com.vichovong.restaurant_pos.feature.table.service;

import com.vichovong.restaurant_pos.common.dto.PageResponse;
import com.vichovong.restaurant_pos.feature.table.dto.TableCreateRequest;
import com.vichovong.restaurant_pos.feature.table.dto.TableResponse;
import com.vichovong.restaurant_pos.feature.table.dto.TableUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DiningTableService {

    PageResponse<TableResponse> getAll(Pageable pageable);

    TableResponse getById(UUID id);

    TableResponse create(TableCreateRequest request);

    TableResponse update(UUID id, TableUpdateRequest request);

    TableResponse regenerateQrToken(UUID id);

    void delete(UUID id);
}
