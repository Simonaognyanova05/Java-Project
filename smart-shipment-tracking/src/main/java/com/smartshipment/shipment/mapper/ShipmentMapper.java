package com.smartshipment.shipment.mapper;

import org.springframework.stereotype.Component;
import com.smartshipment.customer.entity.Customer;
import com.smartshipment.shipment.dto.ShipmentRequest;
import com.smartshipment.shipment.dto.ShipmentResponse;
import com.smartshipment.shipment.entity.Shipment;

@Component
public class ShipmentMapper {
    public Shipment toEntity(ShipmentRequest request, Customer customer) {
        Shipment shipment = new Shipment(customer, request.weightKg());
        shipment.setRecipientName(request.recipientName());
        shipment.setDeliveryAddress(request.deliveryAddress());
        shipment.setExpectedDeliveryAt(request.expectedDeliveryAt());
        return shipment;
    }

    public ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(shipment.getId(), shipment.getTrackingNumber(), shipment.getRecipientName(),
                shipment.getDeliveryAddress(), shipment.getWeightKg(),
                shipment.getTrip() == null ? null : shipment.getTrip().getId(), shipment.getCustomer().getId(),
                shipment.getStatus(), shipment.getExpectedDeliveryAt(), shipment.getVersion());
    }
}
