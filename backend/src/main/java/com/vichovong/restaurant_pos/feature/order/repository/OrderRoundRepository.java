package com.vichovong.restaurant_pos.feature.order.repository;

import com.vichovong.restaurant_pos.feature.order.entity.OrderRound;
import com.vichovong.restaurant_pos.feature.order.entity.FulfillmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderRoundRepository extends JpaRepository<OrderRound, UUID> {

    List<OrderRound> findBySessionIdOrderByRoundNumberAsc(UUID sessionId);

    // sentAt IS the FIFO cook queue (cashier spec §3)
    // sentAt IS the FIFO cook queue (cashier spec §3)
    @Query("SELECT DISTINCT r FROM OrderRound r JOIN r.lines l WHERE l.status IN :lineStatuses AND l.station = :station AND l.voidedAt IS NULL ORDER BY r.sentAt ASC")
    List<OrderRound> findByLineStatusesAndStationOrderBySentAtAsc(@Param("lineStatuses") Collection<com.vichovong.restaurant_pos.feature.order.entity.LineItemStatus> lineStatuses, @Param("station") com.vichovong.restaurant_pos.feature.menu.entity.StationType station);

    List<OrderRound> findByFulfillmentStatusOrderBySentAtAsc(FulfillmentStatus status);

    List<OrderRound> findBySessionIdIn(Collection<UUID> sessionIds);

    @Query("select coalesce(max(r.roundNumber), 0) from OrderRound r where r.session.id = :sessionId")
    int findMaxRoundNumber(@Param("sessionId") UUID sessionId);

    // Device ids are minted per scan, so a single hit means the device already sent
    boolean existsByDeviceId(UUID deviceId);

    @Query("select count(l) > 0 from OrderRoundLineItem l where l.voidedBy.id = :userId")
    boolean existsByVoidedById(@Param("userId") UUID userId);
}
