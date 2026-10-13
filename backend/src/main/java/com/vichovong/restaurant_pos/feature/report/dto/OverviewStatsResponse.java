package com.vichovong.restaurant_pos.feature.report.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OverviewStatsResponse {
    private BigDecimal todayRevenue;
    private int activeTables;
    private int openTickets;
    private BigDecimal monthlyRevenue;
    private int monthlyVoids;
    private List<TopSellerItem> topSellers;
    private String activePromoTitle;

    @Data
    public static class TopSellerItem {
        private String itemName;
        private int count;
    }
}
