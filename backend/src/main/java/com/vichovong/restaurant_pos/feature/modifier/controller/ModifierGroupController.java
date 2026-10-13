package com.vichovong.restaurant_pos.feature.modifier.controller;

import com.vichovong.restaurant_pos.common.dto.ApiResponse;
import com.vichovong.restaurant_pos.common.dto.PageResponse;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierGroupCreateRequest;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierGroupResponse;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierGroupUpdateRequest;
import com.vichovong.restaurant_pos.feature.modifier.service.ModifierGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/modifier-groups")
@RequiredArgsConstructor
public class ModifierGroupController {

    private final ModifierGroupService modifierGroupService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ModifierGroupResponse>>> getAll(
            @PageableDefault(size = 20, sort = "nameEn") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(modifierGroupService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ModifierGroupResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(modifierGroupService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModifierGroupResponse>> create(
            @Valid @RequestBody ModifierGroupCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Modifier group created", modifierGroupService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModifierGroupResponse>> update(@PathVariable UUID id,
                                                                     @Valid @RequestBody ModifierGroupUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Modifier group updated", modifierGroupService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        modifierGroupService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Modifier group deleted", null));
    }

    @PostMapping(value = "/{groupId}/options/{optionId}/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.vichovong.restaurant_pos.feature.modifier.dto.ModifierOptionResponse>> uploadOptionImage(
            @PathVariable UUID groupId,
            @PathVariable UUID optionId,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Image uploaded", modifierGroupService.uploadOptionImage(groupId, optionId, file)));
    }
}
