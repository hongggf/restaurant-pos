package com.vichovong.restaurant_pos.feature.report.service.impl;

import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;
import com.vichovong.restaurant_pos.feature.report.dto.OverviewStatsResponse;
import com.vichovong.restaurant_pos.feature.report.service.ReportService;
import com.vichovong.restaurant_pos.feature.table.entity.SessionStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public OverviewStatsResponse getOverviewStats() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();
        
        YearMonth thisMonth = YearMonth.now(zone);
        Instant startOfMonth = thisMonth.atDay(1).atStartOfDay(zone).toInstant();
        Instant endOfMonth = thisMonth.plusMonths(1).atDay(1).atStartOfDay(zone).minusNanos(1).toInstant();

        // todayRevenue
        BigDecimal todayRevenue = entityManager.createQuery(
                "SELECT SUM(p.billTotal) FROM Payment p WHERE p.paidAt BETWEEN :start AND :end", BigDecimal.class)
                .setParameter("start", startOfDay)
                .setParameter("end", endOfDay)
                .getSingleResult();
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        // monthlyRevenue
        BigDecimal monthlyRevenue = entityManager.createQuery(
                "SELECT SUM(p.billTotal) FROM Payment p WHERE p.paidAt BETWEEN :start AND :end", BigDecimal.class)
                .setParameter("start", startOfMonth)
                .setParameter("end", endOfMonth)
                .getSingleResult();
        if (monthlyRevenue == null) monthlyRevenue = BigDecimal.ZERO;

        // activeTables
        Long activeTables = entityManager.createQuery(
                "SELECT COUNT(t) FROM TableSession t WHERE t.status = :status", Long.class)
                .setParameter("status", SessionStatus.ACTIVE)
                .getSingleResult();

        // openTickets
        Long openTickets = entityManager.createQuery(
                "SELECT COUNT(o) FROM OrderRound o WHERE o.fulfillmentStatus IN (:status1, :status2)", Long.class)
                .setParameter("status1", FulfillmentStatus.NEW)
                .setParameter("status2", FulfillmentStatus.READY)
                .getSingleResult();

        // monthlyVoids
        Long monthlyVoids = entityManager.createQuery(
                "SELECT COUNT(l) FROM OrderRoundLineItem l WHERE l.voidedAt IS NOT NULL AND l.voidedAt BETWEEN :start AND :end", Long.class)
                .setParameter("start", startOfMonth)
                .setParameter("end", endOfMonth)
                .getSingleResult();

        // topSellers
        List<Object[]> topSellersObj = entityManager.createQuery(
                "SELECT l.nameEn, SUM(l.quantity) as cnt FROM OrderRoundLineItem l " +
                "WHERE l.voidedAt IS NULL AND l.createdAt BETWEEN :start AND :end " +
                "GROUP BY l.nameEn ORDER BY cnt DESC", Object[].class)
                .setParameter("start", startOfMonth)
                .setParameter("end", endOfMonth)
                .setMaxResults(5)
                .getResultList();

        List<OverviewStatsResponse.TopSellerItem> topSellers = topSellersObj.stream().map(obj -> {
            OverviewStatsResponse.TopSellerItem item = new OverviewStatsResponse.TopSellerItem();
            item.setItemName((String) obj[0]);
            item.setCount(((Number) obj[1]).intValue());
            return item;
        }).collect(Collectors.toList());

        // activePromoTitle
        List<String> promos = entityManager.createQuery(
                "SELECT g.title FROM GuestPromo g WHERE g.isActive = true", String.class)
                .setMaxResults(1)
                .getResultList();
        String activePromoTitle = promos.isEmpty() ? null : promos.get(0);

        OverviewStatsResponse res = new OverviewStatsResponse();
        res.setTodayRevenue(todayRevenue);
        res.setMonthlyRevenue(monthlyRevenue);
        res.setActiveTables(activeTables != null ? activeTables.intValue() : 0);
        res.setOpenTickets(openTickets != null ? openTickets.intValue() : 0);
        res.setMonthlyVoids(monthlyVoids != null ? monthlyVoids.intValue() : 0);
        res.setTopSellers(topSellers);
        res.setActivePromoTitle(activePromoTitle);

        return res;
    }
}
