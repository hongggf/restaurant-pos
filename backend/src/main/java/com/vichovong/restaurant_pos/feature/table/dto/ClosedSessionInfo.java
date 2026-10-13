package com.vichovong.restaurant_pos.feature.table.dto;
import java.util.UUID;
public record ClosedSessionInfo(UUID tableId, UUID sessionId, boolean hasReceipt) {}
