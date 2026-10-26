package com.smartshipment.shipment.dto;

import java.math.BigDecimal;

public record ShipmentResponse(
		Long id,
		String trackingNumber,
		String recipientName,
		String deliveryAddress,
		BigDecimal weightKg,
		Long tripId) {
}
