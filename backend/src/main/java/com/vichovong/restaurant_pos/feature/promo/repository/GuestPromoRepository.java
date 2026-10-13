package com.vichovong.restaurant_pos.feature.promo.repository;

import com.vichovong.restaurant_pos.feature.promo.entity.GuestPromo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuestPromoRepository extends JpaRepository<GuestPromo, Long> {
    Optional<GuestPromo> findByIsActiveTrue();
}
