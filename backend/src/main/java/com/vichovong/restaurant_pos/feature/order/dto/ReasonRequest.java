package com.vichovong.restaurant_pos.feature.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cancel/void require a reason — the audit trail for disputes and theft detection. */
public record ReasonRequest(
        @NotBlank @Size(max = 200) String reason
) {
}
