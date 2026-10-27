package com.smartshipment.tracking.dto;

import java.time.Instant;

import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.tracking.entity.TrackingEvent;

public record PublicTrackingEventResponse(
		ShipmentStatus previousStatus,
		ShipmentStatus newStatus,
		Instant occurredAt) {
	public static PublicTrackingEventResponse from(TrackingEvent event) {
		return new PublicTrackingEventResponse(event.getPreviousStatus(), event.getNewStatus(), event.getOccurredAt());
	}
}
