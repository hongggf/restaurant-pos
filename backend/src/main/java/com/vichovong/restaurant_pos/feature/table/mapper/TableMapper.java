package com.vichovong.restaurant_pos.feature.table.mapper;

import com.vichovong.restaurant_pos.feature.table.dto.TableResponse;
import com.vichovong.restaurant_pos.feature.table.entity.DiningTable;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TableMapper {

    TableResponse toResponse(DiningTable table);
}
