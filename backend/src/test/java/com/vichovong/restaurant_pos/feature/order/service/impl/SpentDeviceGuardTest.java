package com.vichovong.restaurant_pos.feature.order.service.impl;

import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.order.repository.OrderRoundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpentDeviceGuardTest {

    @Mock
    private OrderRoundRepository orderRoundRepository;

    @InjectMocks
    private SpentDeviceGuard spentDeviceGuard;

    @Test
    @DisplayName("requireNotSpent: passes when device has not sent an order round")
    void requireNotSpent_deviceNotSpent_doesNotThrow() {
        UUID deviceId = UUID.randomUUID();
        when(orderRoundRepository.existsByDeviceId(deviceId)).thenReturn(false);

        assertThatCode(() -> spentDeviceGuard.requireNotSpent(deviceId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireNotSpent: throws FORBIDDEN when device has already sent an order round")
    void requireNotSpent_deviceAlreadySpent_throwsForbidden() {
        UUID deviceId = UUID.randomUUID();
        when(orderRoundRepository.existsByDeviceId(deviceId)).thenReturn(true);

        assertThatThrownBy(() -> spentDeviceGuard.requireNotSpent(deviceId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getMessage()).isEqualTo(SpentDeviceGuard.MESSAGE);
                });
    }
}
