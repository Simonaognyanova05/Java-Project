package com.smartshipment.tracking.dto;

import java.time.Instant;
import java.util.List;

import com.smartshipment.shipment.entity.ShipmentStatus;

public record PublicTrackingResponse(
		String trackingNumber,
		ShipmentStatus status,
		Instant createdAt,
		Instant expectedDeliveryAt,
		List<PublicTrackingEventResponse> history) {
}
