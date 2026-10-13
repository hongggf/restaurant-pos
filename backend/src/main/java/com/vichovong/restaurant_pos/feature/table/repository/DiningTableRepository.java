package com.vichovong.restaurant_pos.feature.table.repository;

import com.vichovong.restaurant_pos.feature.table.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DiningTableRepository extends JpaRepository<DiningTable, UUID> {

    Optional<DiningTable> findByQrToken(String qrToken);

    boolean existsByTableNumber(String tableNumber);

    boolean existsByTableNumberAndIdNot(String tableNumber, UUID id);
}
