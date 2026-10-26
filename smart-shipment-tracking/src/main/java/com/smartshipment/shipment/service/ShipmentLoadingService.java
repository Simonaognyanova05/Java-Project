package com.smartshipment.shipment.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import com.smartshipment.employee.entity.Employee;
import com.smartshipment.employee.repository.EmployeeRepository;
import com.smartshipment.shipment.dto.ShipmentAssignmentResponse;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.tracking.entity.TrackingEvent;
import com.smartshipment.tracking.repository.TrackingEventRepository;
import com.smartshipment.trip.entity.Trip;
import com.smartshipment.trip.repository.TripRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ShipmentLoadingService {
    private final ShipmentRepository shipments;
    private final TripRepository trips;
    private final EmployeeRepository employees;
    private final TrackingEventRepository events;
    private final Clock clock;

    public ShipmentLoadingService(ShipmentRepository shipments, TripRepository trips,
            EmployeeRepository employees, TrackingEventRepository events, Clock clock) {
        this.shipments = shipments;
        this.trips = trips;
        this.employees = employees;
        this.events = events;
        this.clock = clock;
    }

    public ShipmentAssignmentResponse load(@NotNull @Positive Long shipmentId,
            @NotNull @Positive Long employeeId, @NotNull @PositiveOrZero Long expectedVersion) {
        // Same shared locking protocol as assignment: shipment, then trip.
        Shipment shipment = shipments.findForUpdate(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
        if (!Objects.equals(shipment.getVersion(), expectedVersion)) {
            throw conflict("Shipment was modified; reload it before loading");
        }
        if (shipment.getStatus() != ShipmentStatus.ACCEPTED) {
            throw conflict("Only ACCEPTED shipments can be loaded");
        }
        if (shipment.getTrip() == null) {
            throw conflict("Shipment must have an assigned trip");
        }
        Trip trip = trips.findByIdForUpdate(shipment.getTrip().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
        if (trip.isDeleted() || trip.getVehicle() == null || trip.getVehicle().isDeleted()) {
            throw conflict("Loading requires an active trip and vehicle");
        }
        // Assignment already reserves weight, so loading must not add it again.
        if (shipments.totalAssignedWeight(trip.getId()).compareTo(trip.getVehicle().getCapacityKg()) > 0) {
            throw conflict("Trip capacity is exceeded");
        }
        Employee employee = employees.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active employee not found"));
        shipment.markLoaded();
        events.saveAndFlush(new TrackingEvent(shipment, ShipmentStatus.ACCEPTED, ShipmentStatus.LOADED,
                Instant.now(clock), employee));
        return ShipmentAssignmentResponse.from(shipment);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
