package com.vichovong.restaurant_pos.feature.promo.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.feature.promo.dto.GuestPromoDto;
import com.vichovong.restaurant_pos.feature.promo.entity.GuestPromo;
import com.vichovong.restaurant_pos.feature.promo.repository.GuestPromoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promos")
@RequiredArgsConstructor
public class GuestPromoController {

    private final GuestPromoRepository repository;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<GuestPromo>> getActivePromo() {
        // Return the first active promo if there are multiple
        return repository.findAll().stream().filter(GuestPromo::isActive).findFirst()
                .map(promo -> ResponseEntity.ok(ApiResponse.success(promo)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GuestPromo>>> getAllPromos() {
        return ResponseEntity.ok(ApiResponse.success(repository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GuestPromo>> createPromo(@RequestBody GuestPromoDto dto) {
        if (dto.isActive()) {
            deactivateAll();
        }
        GuestPromo promo = GuestPromo.builder()
                .title(dto.getTitle())
                .imageUrl(dto.getImageUrl())
                .isActive(dto.isActive())
                .build();
        return ResponseEntity.ok(ApiResponse.success(repository.save(promo)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GuestPromo>> updatePromo(@PathVariable Long id, @RequestBody GuestPromoDto dto) {
        return repository.findById(id).map(promo -> {
            if (dto.isActive() && !promo.isActive()) {
                deactivateAll();
            }
            promo.setTitle(dto.getTitle());
            promo.setImageUrl(dto.getImageUrl());
            promo.setActive(dto.isActive());
            return ResponseEntity.ok(ApiResponse.success(repository.save(promo)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePromo(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private void deactivateAll() {
        List<GuestPromo> activePromos = repository.findAll().stream().filter(GuestPromo::isActive).toList();
        for (GuestPromo p : activePromos) {
            p.setActive(false);
            repository.save(p);
        }
    }
}
