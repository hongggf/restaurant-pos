package com.vichovong.restaurant_pos.feature.modifier.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.feature.modifier.dto.AttachModifierGroupRequest;
import com.vichovong.restaurant_pos.feature.modifier.dto.AttachedModifierGroupResponse;
import com.vichovong.restaurant_pos.feature.modifier.service.MenuItemModifierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menu-items/{menuItemId}/modifier-groups")
@RequiredArgsConstructor
public class MenuItemModifierController {

    private final MenuItemModifierService menuItemModifierService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AttachedModifierGroupResponse>>> getForMenuItem(
            @PathVariable UUID menuItemId) {
        return ResponseEntity.ok(ApiResponse.success(menuItemModifierService.getForMenuItem(menuItemId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttachedModifierGroupResponse>> attach(@PathVariable UUID menuItemId,
                                                                             @Valid @RequestBody AttachModifierGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Modifier group attached", menuItemModifierService.attach(menuItemId, request)));
    }

    @DeleteMapping("/{modifierGroupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> detach(@PathVariable UUID menuItemId,
                                                    @PathVariable UUID modifierGroupId) {
        menuItemModifierService.detach(menuItemId, modifierGroupId);
        return ResponseEntity.ok(ApiResponse.success("Modifier group detached", null));
    }
}
