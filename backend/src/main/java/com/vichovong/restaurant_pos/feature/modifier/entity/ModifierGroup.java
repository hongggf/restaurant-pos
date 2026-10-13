package com.vichovong.restaurant_pos.feature.modifier.entity;

import com.vichovong.restaurant_pos.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "modifier_groups")
public class ModifierGroup extends BaseEntity {

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_km", nullable = false, length = 150)
    private String nameKm;

    // minChoice >= 1 means the group is required; there is no separate "required" flag
    @Column(name = "min_choice", nullable = false)
    private int minChoice;

    @Column(name = "max_choice")
    private Integer maxChoice;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "modifierGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ModifierOption> options = new ArrayList<>();
}
