package com.vichovong.restaurant_pos.feature.cart.service.impl;

import com.vichovong.restaurant_pos.feature.modifier.entity.ModifierGroup;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/**
 * A built bowl has to carry a minimum of actual food before it can be ordered.
 *
 * <p>DIY Malatang is a free base and a flavour costs a nominal $0.01, so
 * without this a guest could order a cent's worth of plain broth. The floor is
 * counted only over the groups that make up the meal — flavour is nominal and
 * already required by its own minChoice, while Extra Love Add-Ons are extras,
 * so a drink cannot be used to clear it.
 *
 * <p>Groups are matched by name, not by menu item id. Today these groups hang
 * only off DIY Malatang, so the rule applies there and nowhere else; a future
 * build-your-own item that reuses them inherits the same floor. An item with
 * none of these groups is unaffected.
 *
 * <p>Mirrors {@code frontend/src/features/modifiers/lib/build-minimum.ts},
 * which greys out the Add button before a request is ever sent. This class is
 * the enforcement; that one is the explanation.
 */
final class BuildMinimumPolicy {

    static final BigDecimal MINIMUM = new BigDecimal("3.00");

    private static final Set<String> COUNTED_GROUPS =
            Set.of("meat", "meat ball", "veggie", "noodles & rice");

    private BuildMinimumPolicy() {
    }

    /** True when this group's selections count toward the minimum. */
    static boolean counts(ModifierGroup group) {
        return group != null && COUNTED_GROUPS.contains(normalize(group.getNameEn()));
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
