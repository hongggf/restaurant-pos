package com.vichovong.restaurant_pos.feature.cart.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.cart.dto.CartResponse;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineItem;
import com.vichovong.restaurant_pos.feature.cart.entity.CartLineModifierSelection;
import com.vichovong.restaurant_pos.feature.currency.service.ExchangeRateService;
import com.vichovong.restaurant_pos.feature.menu.entity.MenuItem;
import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierOption;
import com.vichovong.restaurant_pos.feature.table.entity.TableSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartPricingServiceImplTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private CartPricingServiceImpl cartPricingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cartPricingService, "vatRate", new BigDecimal("0.10"));
    }

    @Test
    @DisplayName("Should correctly calculate pricing for item without modifiers")
    void price_itemWithoutModifiers_calculatesSubtotalVatAndTotal() {
        TableSession session = new TableSession();
        session.setId(UUID.randomUUID());

        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        item.setNameEn("Beef Noodle");
        item.setNameKm("គុយទាវសាច់គោ");
        item.setPrice(new BigDecimal("5.00"));

        CartLineItem line = new CartLineItem();
        line.setId(UUID.randomUUID());
        line.setMenuItem(item);
        line.setQuantity(2);
        line.setSelections(new ArrayList<>());

        when(exchangeRateService.convert(new BigDecimal("11.00"), "USD", "KHR"))
                .thenReturn(new BigDecimal("44000.00"));

        CartResponse response = cartPricingService.price(session, List.of(line));

        assertThat(response.sessionId()).isEqualTo(session.getId());
        assertThat(response.currencyCode()).isEqualTo("USD");
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(response.vatRate()).isEqualByComparingTo(new BigDecimal("0.10"));
        assertThat(response.vatAmount()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(response.grandTotal()).isEqualByComparingTo(new BigDecimal("11.00"));
        assertThat(response.grandTotalKhr()).isEqualByComparingTo(new BigDecimal("44000.00"));
        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).unitPrice()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(response.lines().get(0).lineTotal()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Should include modifier option prices multiplied by selection quantities in unit price")
    void price_itemWithModifiers_addsModifierPricesToUnitPrice() {
        TableSession session = new TableSession();
        session.setId(UUID.randomUUID());

        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        item.setNameEn("Malatang Base Bowl");
        item.setNameKm("ស៊ុបម៉ាឡា");
        item.setPrice(new BigDecimal("3.00"));

        ModifierOption extraBeef = new ModifierOption();
        extraBeef.setId(UUID.randomUUID());
        extraBeef.setNameEn("Extra Beef");
        extraBeef.setNameKm("សាច់គោបន្ថែម");
        extraBeef.setUnitPrice(new BigDecimal("1.50"));

        ModifierOption cheese = new ModifierOption();
        cheese.setId(UUID.randomUUID());
        cheese.setNameEn("Cheese");
        cheese.setNameKm("ឈីស");
        cheese.setUnitPrice(new BigDecimal("0.75"));

        CartLineItem line = new CartLineItem();
        line.setId(UUID.randomUUID());
        line.setMenuItem(item);
        line.setQuantity(2);

        CartLineModifierSelection sel1 = new CartLineModifierSelection();
        sel1.setModifierOption(extraBeef);
        sel1.setQuantity(2); // 2 * 1.50 = 3.00

        CartLineModifierSelection sel2 = new CartLineModifierSelection();
        sel2.setModifierOption(cheese);
        sel2.setQuantity(1); // 1 * 0.75 = 0.75

        line.setSelections(List.of(sel1, sel2));

        CartResponse response = cartPricingService.price(session, List.of(line));

        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("13.50"));
        assertThat(response.vatAmount()).isEqualByComparingTo(new BigDecimal("1.35"));
        assertThat(response.grandTotal()).isEqualByComparingTo(new BigDecimal("14.85"));
        assertThat(response.lines().get(0).unitPrice()).isEqualByComparingTo(new BigDecimal("6.75"));
        assertThat(response.lines().get(0).lineTotal()).isEqualByComparingTo(new BigDecimal("13.50"));
        assertThat(response.lines().get(0).selections()).hasSize(2);
    }

    @Test
    @DisplayName("Should return null for grandTotalKhr if ExchangeRateService throws ApiException")
    void price_exchangeRateNotFound_setsGrandTotalKhrToNull() {
        TableSession session = new TableSession();
        session.setId(UUID.randomUUID());

        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        item.setNameEn("Water");
        item.setPrice(new BigDecimal("1.00"));

        CartLineItem line = new CartLineItem();
        line.setId(UUID.randomUUID());
        line.setMenuItem(item);
        line.setQuantity(1);
        line.setSelections(List.of());

        when(exchangeRateService.convert(any(), eq("USD"), eq("KHR")))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "No rate configured"));

        CartResponse response = cartPricingService.price(session, List.of(line));

        assertThat(response.grandTotal()).isEqualByComparingTo(new BigDecimal("1.10"));
        assertThat(response.grandTotalKhr()).isNull();
    }
}
