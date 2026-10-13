package com.vichovong.restaurant_pos.feature.modifier.entity;

import com.vichovong.restaurant_pos.common.entity.BaseEntity;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "menu_item_modifier_groups",
        uniqueConstraints = @UniqueConstraint(name = "uq_menu_item_modifier_group",
                columnNames = {"menu_item_id", "modifier_group_id"}))
public class MenuItemModifierGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modifier_group_id", nullable = false)
    private ModifierGroup modifierGroup;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
