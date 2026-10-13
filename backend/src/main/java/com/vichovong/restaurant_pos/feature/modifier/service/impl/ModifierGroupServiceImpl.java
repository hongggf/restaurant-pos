package com.vichovong.restaurant_pos.feature.modifier.service.impl;

import com.vichovong.restaurant_pos.common.dto.PageResponse;
import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierGroupCreateRequest;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierGroupResponse;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierGroupUpdateRequest;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierOptionRequest;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierGroup;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import com.vichovong.restaurant_pos.feature.modifier.mapper.ModifierMapper;
import com.vichovong.restaurant_pos.feature.modifier.repository.MenuItemModifierGroupRepository;
import com.vichovong.restaurant_pos.feature.modifier.repository.ModifierGroupRepository;
import com.vichovong.restaurant_pos.feature.modifier.service.ModifierGroupService;
import lombok.RequiredArgsConstructor;
import com.vichovong.restaurant_pos.feature.modifier.dto.ModifierOptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModifierGroupServiceImpl implements ModifierGroupService {

    private final ModifierGroupRepository modifierGroupRepository;
    private final MenuItemModifierGroupRepository menuItemModifierGroupRepository;
    private final ModifierMapper modifierMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public PageResponse<ModifierGroupResponse> getAll(Pageable pageable) {
        return PageResponse.from(modifierGroupRepository.findAll(pageable).map(modifierMapper::toResponse));
    }

    @Override
    public ModifierGroupResponse getById(UUID id) {
        return modifierMapper.toResponse(findGroup(id));
    }

    @Override
    @Transactional
    public ModifierGroupResponse create(ModifierGroupCreateRequest request) {
        validateChoices(request.minChoice(), request.maxChoice());
        ModifierGroup group = new ModifierGroup();
        applyGroupFields(group, request.nameEn(), request.nameKm(), request.minChoice(),
                request.maxChoice(), request.active());
        for (ModifierOptionRequest optionRequest : request.options()) {
            ModifierOption option = new ModifierOption();
            option.setModifierGroup(group);
            applyOptionFields(option, optionRequest);
            group.getOptions().add(option);
        }
        return modifierMapper.toResponse(modifierGroupRepository.save(group));
    }

    @Override
    @Transactional
    public ModifierGroupResponse update(UUID id, ModifierGroupUpdateRequest request) {
        validateChoices(request.minChoice(), request.maxChoice());
        ModifierGroup group = findGroup(id);
        applyGroupFields(group, request.nameEn(), request.nameKm(), request.minChoice(),
                request.maxChoice(), request.active());
        syncOptions(group, request.options());
        return modifierMapper.toResponse(group);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ModifierGroup group = findGroup(id);
        if (menuItemModifierGroupRepository.existsByModifierGroupId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot delete modifier group attached to menu items: " + id);
        }
        modifierGroupRepository.delete(group);
    }

    /**
     * Options with an id are updated, options without an id are created,
     * existing options missing from the request are removed (orphanRemoval).
     */
    private void syncOptions(ModifierGroup group, List<ModifierOptionRequest> requests) {
        List<UUID> submittedIds = requests.stream()
                .map(ModifierOptionRequest::id)
                .filter(Objects::nonNull)
                .toList();
        if (submittedIds.size() != Set.copyOf(submittedIds).size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate option ids in request");
        }
        Map<UUID, ModifierOption> existingById = group.getOptions().stream()
                .collect(Collectors.toMap(ModifierOption::getId, Function.identity()));
        for (UUID submittedId : submittedIds) {
            if (!existingById.containsKey(submittedId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Option does not belong to this group: " + submittedId);
            }
        }
        group.getOptions().removeIf(option -> !submittedIds.contains(option.getId()));
        for (ModifierOptionRequest request : requests) {
            ModifierOption option;
            if (request.id() != null) {
                option = existingById.get(request.id());
            } else {
                option = new ModifierOption();
                option.setModifierGroup(group);
                group.getOptions().add(option);
            }
            applyOptionFields(option, request);
        }
    }

    private void validateChoices(int minChoice, Integer maxChoice) {
        if (maxChoice != null && maxChoice < Math.max(1, minChoice)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "maxChoice must be at least " + Math.max(1, minChoice));
        }
    }

    private void applyGroupFields(ModifierGroup group, String nameEn, String nameKm,
                                  int minChoice, Integer maxChoice, boolean active) {
        group.setNameEn(nameEn);
        group.setNameKm(nameKm);
        group.setMinChoice(minChoice);
        group.setMaxChoice(maxChoice);
        group.setActive(active);
    }

    private void applyOptionFields(ModifierOption option, ModifierOptionRequest request) {
        option.setNameEn(request.nameEn());
        option.setNameKm(request.nameKm());
        option.setImageUrl(request.imageUrl());
        option.setUnitPrice(request.unitPrice());
        option.setPackSize(request.packSize());
        option.setAvailable(request.available());
        option.setSortOrder(request.sortOrder());
    }

    private ModifierGroup findGroup(UUID id) {
        return modifierGroupRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Modifier group not found: " + id));
    }

    @Override
    @Transactional
    public ModifierOptionResponse uploadOptionImage(UUID groupId, UUID optionId, MultipartFile file) {
        ModifierGroup group = findGroup(groupId);
        ModifierOption targetOption = group.getOptions().stream()
                .filter(opt -> opt.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Modifier option not found in this group: " + optionId));

        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        try {
            Path dir = Path.of(uploadDir, "modifier-options", optionId.toString());
            Path cardDir = dir.resolve("card");
            Path thumbDir = dir.resolve("thumb");
            Files.createDirectories(dir);
            Files.createDirectories(cardDir);
            Files.createDirectories(thumbDir);

            String filename = UUID.randomUUID() + "-" + org.springframework.util.StringUtils.cleanPath(
                    org.springframework.util.StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "image");
            
            Path baseTarget = dir.resolve(filename);
            Path cardTarget = cardDir.resolve(filename);
            Path thumbTarget = thumbDir.resolve(filename);

            net.coobird.thumbnailator.Thumbnails.of(file.getInputStream())
                    .useExifOrientation(true)
                    .size(800, 800)
                    .toFile(baseTarget.toFile());

            net.coobird.thumbnailator.Thumbnails.of(baseTarget.toFile())
                    .size(512, 512)
                    .toFile(cardTarget.toFile());

            net.coobird.thumbnailator.Thumbnails.of(baseTarget.toFile())
                    .size(96, 96)
                    .toFile(thumbTarget.toFile());

            targetOption.setImageUrl("/uploads/modifier-options/" + optionId + "/" + filename);
        } catch (net.coobird.thumbnailator.tasks.UnsupportedFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported image format or invalid image");
        } catch (java.io.IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image: " + e.getMessage());
        }

        return modifierMapper.toResponse(targetOption);
    }
}
