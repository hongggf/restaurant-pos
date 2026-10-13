package com.vichovong.restaurant_pos.feature.promo.dto;

import lombok.Data;

@Data
public class GuestPromoDto {
    private String title;
    private String imageUrl;
    private boolean isActive;
}
