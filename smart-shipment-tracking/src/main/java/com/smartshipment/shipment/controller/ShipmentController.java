package com.smartshipment.shipment.controller;

import java.net.URI;
import java.util.List;
import com.smartshipment.shipment.dto.ShipmentRequest;
import com.smartshipment.shipment.dto.ShipmentResponse;
import com.smartshipment.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> create(@Valid @RequestBody ShipmentRequest request) {
        ShipmentResponse created = shipmentService.create(request);
        return ResponseEntity.created(URI.create("/api/shipments/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public ShipmentResponse findById(@PathVariable @Positive Long id) {
        return shipmentService.findById(id);
    }

    @PutMapping("/{shipmentId}/trip/{tripId}")
    public ShipmentResponse assignToTrip(@PathVariable @Positive Long shipmentId,
            @PathVariable @Positive Long tripId, @RequestParam @PositiveOrZero Long expectedVersion) {
        return shipmentService.assignToTrip(shipmentId, tripId, expectedVersion);
    }

    @GetMapping("/trip/{tripId}")
    public List<ShipmentResponse> findByTripId(@PathVariable @Positive Long tripId) {
        return shipmentService.findByTripId(tripId);
    }
}
