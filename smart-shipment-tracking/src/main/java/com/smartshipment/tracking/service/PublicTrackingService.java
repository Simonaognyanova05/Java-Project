package com.smartshipment.tracking.service;

import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.tracking.dto.PublicTrackingEventResponse;
import com.smartshipment.tracking.dto.PublicTrackingResponse;
import com.smartshipment.tracking.repository.TrackingEventRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicTrackingService {
	private final ShipmentRepository shipments;
	private final TrackingEventRepository events;

	public PublicTrackingService(ShipmentRepository shipments, TrackingEventRepository events) {
		this.shipments = shipments;
		this.events = events;
	}

	@Transactional(readOnly = true)
	public PublicTrackingResponse findByTrackingNumber(String trackingNumber) {
		var shipment = shipments.findByTrackingNumber(trackingNumber)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
		var history = events.findAllByShipmentIdOrderByOccurredAtAscIdAsc(shipment.getId()).stream()
				.map(PublicTrackingEventResponse::from)
				.toList();
		return new PublicTrackingResponse(shipment.getTrackingNumber(), shipment.getStatus(), shipment.getCreatedAt(),
				shipment.getExpectedDeliveryAt(), history);
	}
}
