package com.smartshipment.shipment.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import com.smartshipment.shipment.dto.AssignTripRequest;
import com.smartshipment.shipment.dto.ShipmentAssignmentResponse;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.trip.entity.Trip;
import com.smartshipment.trip.repository.TripRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional
public class ShipmentAssignmentService {
    private final ShipmentRepository shipments;
    private final TripRepository trips;

    public ShipmentAssignmentService(ShipmentRepository shipments, TripRepository trips) {
        this.shipments = shipments;
        this.trips = trips;
    }

    public ShipmentAssignmentResponse assign(@Positive Long shipmentId, @Valid AssignTripRequest request) {
        // Shared lock order: shipment first, then old/new trip IDs in ascending order.
        Shipment shipment = shipments.findForUpdate(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
        if (!Objects.equals(shipment.getVersion(), request.expectedVersion())) {
            throw conflict("Shipment was modified; reload it before assigning a trip");
        }
        if (shipment.getStatus() != ShipmentStatus.CREATED && shipment.getStatus() != ShipmentStatus.ACCEPTED) {
            throw conflict("Only CREATED or ACCEPTED shipments can change trips");
        }
        Long oldTripId = shipment.getTrip() == null ? null : shipment.getTrip().getId();
        TreeSet<Long> tripIds = new TreeSet<>();
        tripIds.add(request.tripId());
        if (oldTripId != null) {
            tripIds.add(oldTripId);
        }
        Trip target = null;
        for (Long tripId : tripIds) {
            Trip locked = trips.findByIdForUpdate(tripId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
            if (tripId.equals(request.tripId())) {
                target = locked;
            }
        }
        if (target.isDeleted()) {
            throw conflict("Cannot assign a shipment to an inactive trip");
        }
        if (target.getVehicle() == null || target.getVehicle().isDeleted()) {
            throw conflict("Trip requires an active vehicle");
        }
        BigDecimal reservedWeight = shipments.totalAssignedWeight(target.getId());
        if (!Objects.equals(oldTripId, target.getId())) {
            reservedWeight = reservedWeight.add(shipment.getWeightKg());
        }
        if (reservedWeight.compareTo(target.getVehicle().getCapacityKg()) > 0) {
            throw conflict("Trip capacity would be exceeded");
        }
        shipment.setTrip(target);
        shipments.flush();
        return ShipmentAssignmentResponse.from(shipment);
    }

    @Transactional(readOnly = true)
    public List<ShipmentAssignmentResponse> findByTrip(@Positive Long tripId) {
        if (trips.findByIdAndDeletedFalse(tripId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found");
        }
        return shipments.findAllByTripIdOrderByIdAsc(tripId).stream()
                .map(ShipmentAssignmentResponse::from).toList();
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
