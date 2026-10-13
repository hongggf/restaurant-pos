package com.vichovong.restaurant_pos.feature.table.entity;

import com.vichovong.restaurant_pos.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dining_tables")
public class DiningTable extends BaseEntity {

    @Column(name = "table_number", nullable = false, unique = true, length = 20)
    private String tableNumber;

    // Random >=128-bit token embedded in the table's QR code; regenerable to invalidate old codes
    @Column(name = "qr_token", nullable = false, unique = true, length = 64)
    private String qrToken;

    @Column(nullable = false)
    private boolean active = true;
}
