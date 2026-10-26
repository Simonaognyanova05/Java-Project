package com.smartshipment.shipment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import com.smartshipment.shipment.entity.ShipmentStatus;

public record ShipmentResponse(Long id, String trackingNumber, String recipientName,
        String deliveryAddress, BigDecimal weightKg, Long tripId, Long customerId,
        ShipmentStatus status, Instant expectedDeliveryAt, Long version) {
}
