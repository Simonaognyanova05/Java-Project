package com.smartshipment.shipment.mapper;

import org.springframework.stereotype.Component;

import com.smartshipment.shipment.dto.ShipmentRequest;
import com.smartshipment.shipment.dto.ShipmentResponse;
import com.smartshipment.shipment.entity.Shipment;

@Component
public class ShipmentMapper {
	public Shipment toEntity(ShipmentRequest request) {
		return new Shipment(request.trackingNumber(), request.recipientName(), request.deliveryAddress(),
				request.weightKg());
	}

	public ShipmentResponse toResponse(Shipment shipment) {
		Long tripId = shipment.getTrip() == null ? null : shipment.getTrip().getId();
		return new ShipmentResponse(shipment.getId(), shipment.getTrackingNumber(), shipment.getRecipientName(),
				shipment.getDeliveryAddress(), shipment.getWeightKg(), tripId);
	}
}
