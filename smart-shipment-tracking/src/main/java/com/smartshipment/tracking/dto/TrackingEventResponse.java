package com.smartshipment.tracking.dto;

import java.time.Instant;

import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.tracking.entity.TrackingEvent;

public record TrackingEventResponse(
		Long id,
		Long shipmentId,
		ShipmentStatus previousStatus,
		ShipmentStatus newStatus,
		Instant occurredAt,
		Long employeeId) {
	public static TrackingEventResponse from(TrackingEvent event) {
		return new TrackingEventResponse(event.getId(), event.getShipment().getId(), event.getPreviousStatus(),
				event.getNewStatus(), event.getOccurredAt(),
				event.getEmployee() == null ? null : event.getEmployee().getId());
	}
}
