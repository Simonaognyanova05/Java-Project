package com.smartshipment.shipment.dto;

import java.math.BigDecimal;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;

public record ShipmentAssignmentResponse(Long id, String trackingNumber, ShipmentStatus status,
        BigDecimal weightKg, Long tripId, Long version) {
    public static ShipmentAssignmentResponse from(Shipment shipment) {
        return new ShipmentAssignmentResponse(shipment.getId(), shipment.getTrackingNumber(),
                shipment.getStatus(), shipment.getWeightKg(),
                shipment.getTrip() == null ? null : shipment.getTrip().getId(), shipment.getVersion());
    }
}
