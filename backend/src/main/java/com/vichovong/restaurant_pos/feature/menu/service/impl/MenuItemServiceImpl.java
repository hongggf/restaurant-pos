package com.vichovong.restaurant_pos.feature.menu.service.impl;

import com.vichovong.restaurant_pos.common.dto.PageResponse;
import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.repository.CartLineItemRepository;
import com.vichovong.restaurant_pos.feature.currency.entity.Currency;
import com.vichovong.restaurant_pos.feature.currency.repository.CurrencyRepository;
import com.vichovong.restaurant_pos.feature.menu.dto.MenuItemCreateRequest;
import com.vichovong.restaurant_pos.feature.menu.dto.MenuItemResponse;
import com.vichovong.restaurant_pos.feature.menu.dto.MenuItemUpdateRequest;
import com.vichovong.restaurant_pos.feature.menu.entity.Category;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.menu.mapper.MenuItemMapper;
import com.vichovong.restaurant_pos.feature.menu.repository.CategoryRepository;
import com.vichovong.restaurant_pos.feature.menu.repository.MenuItemRepository;
import com.vichovong.restaurant_pos.feature.menu.service.MenuItemService;
import com.vichovong.restaurant_pos.feature.modifier.repository.MenuItemModifierGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final CurrencyRepository currencyRepository;
    private final MenuItemModifierGroupRepository menuItemModifierGroupRepository;
    private final CartLineItemRepository cartLineItemRepository;
    private final MenuItemMapper menuItemMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public PageResponse<MenuItemResponse> getAll(Pageable pageable, UUID categoryId, Boolean available) {
        Specification<MenuItem> spec = Specification.where(null);
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (available != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("available"), available));
        }
        return PageResponse.from(menuItemRepository.findAll(spec, pageable).map(menuItemMapper::toResponse));
    }

    @Override
    public MenuItemResponse getById(UUID id) {
        return menuItemMapper.toResponse(findMenuItem(id));
    }

    @Override
    @Transactional
    public MenuItemResponse create(MenuItemCreateRequest request) {
        MenuItem menuItem = new MenuItem();
        applyRequest(menuItem, request.nameEn(), request.nameKm(), request.descriptionEn(), request.descriptionKm(),
                request.price(), request.currencyCode(), request.imageUrl(), request.available(), request.categoryId(), request.station());
        return menuItemMapper.toResponse(menuItemRepository.save(menuItem));
    }

    @Override
    @Transactional
    public MenuItemResponse update(UUID id, MenuItemUpdateRequest request) {
        MenuItem menuItem = findMenuItem(id);
        applyRequest(menuItem, request.nameEn(), request.nameKm(), request.descriptionEn(), request.descriptionKm(),
                request.price(), request.currencyCode(), request.imageUrl(), request.available(), request.categoryId(), request.station());
        return menuItemMapper.toResponse(menuItem);
    }

    @Override
    @Transactional
    public MenuItemResponse updateAvailability(UUID id, boolean available) {
        MenuItem menuItem = findMenuItem(id);
        menuItem.setAvailable(available);
        return menuItemMapper.toResponse(menuItem);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (menuItemModifierGroupRepository.existsByMenuItemId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot delete menu item attached to modifier groups — detach groups first or mark item unavailable");
        }
        cartLineItemRepository.deleteByMenuItemId(id);
        menuItemRepository.delete(findMenuItem(id));
    }

    @Override
    @Transactional
    public MenuItemResponse uploadImage(UUID id, MultipartFile file) {
        MenuItem menuItem = findMenuItem(id);
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        try {
            Path dir = Path.of(uploadDir, "menu-items", id.toString());
            Path cardDir = dir.resolve("card");
            Path thumbDir = dir.resolve("thumb");
            Files.createDirectories(dir);
            Files.createDirectories(cardDir);
            Files.createDirectories(thumbDir);

            String filename = UUID.randomUUID() + "-" + StringUtils.cleanPath(
                    StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "image");
            
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

            menuItem.setImageUrl("/uploads/menu-items/" + id + "/" + filename);
        } catch (net.coobird.thumbnailator.tasks.UnsupportedFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported image format or invalid image");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image: " + e.getMessage());
        }

        return menuItemMapper.toResponse(menuItem);
    }

    private void applyRequest(MenuItem menuItem, String nameEn, String nameKm, String descriptionEn, String descriptionKm,
                               BigDecimal price, String currencyCode, String imageUrl, boolean available, UUID categoryId, com.vichovong.restaurant_pos.feature.menu.entity.StationType station) {
        menuItem.setNameEn(nameEn);
        menuItem.setNameKm(nameKm);
        menuItem.setDescriptionEn(descriptionEn);
        menuItem.setDescriptionKm(descriptionKm);
        menuItem.setPrice(price);
        menuItem.setCurrency(findCurrency(currencyCode));
        if (imageUrl != null) {
            menuItem.setImageUrl(imageUrl.isBlank() ? null : imageUrl);
        }
        menuItem.setAvailable(available);
        menuItem.setCategory(findCategory(categoryId));
        if (station != null) {
            menuItem.setStation(station);
        }
    }

    private MenuItem findMenuItem(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Menu item not found: " + id));
    }

    private Category findCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown category: " + id));
    }

    private Currency findCurrency(String code) {
        return currencyRepository.findByCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown currency code: " + code));
    }
}
