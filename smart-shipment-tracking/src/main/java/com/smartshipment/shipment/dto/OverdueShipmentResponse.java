package com.smartshipment.shipment.dto;

import java.time.Instant;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;

public record OverdueShipmentResponse(Long id, String trackingNumber, ShipmentStatus status,
        Instant expectedDeliveryAt, Long tripId) {
    public static OverdueShipmentResponse from(Shipment shipment) {
        return new OverdueShipmentResponse(shipment.getId(), shipment.getTrackingNumber(), shipment.getStatus(),
                shipment.getExpectedDeliveryAt(), shipment.getTrip() == null ? null : shipment.getTrip().getId());
    }
}
