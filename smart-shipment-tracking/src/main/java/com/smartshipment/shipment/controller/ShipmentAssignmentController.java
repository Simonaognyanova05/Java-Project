package com.smartshipment.shipment.controller;

import java.util.List;
import com.smartshipment.shipment.dto.AssignTripRequest;
import com.smartshipment.shipment.dto.ShipmentAssignmentResponse;
import com.smartshipment.shipment.service.ShipmentAssignmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
public class ShipmentAssignmentController {
    private final ShipmentAssignmentService service;

    public ShipmentAssignmentController(ShipmentAssignmentService service) {
        this.service = service;
    }

    @PutMapping("/api/shipments/{id}/trip")
    public ShipmentAssignmentResponse assign(@PathVariable @Positive Long id,
            @Valid @RequestBody AssignTripRequest request) {
        return service.assign(id, request);
    }

    @GetMapping("/api/trips/{id}/shipments")
    public List<ShipmentAssignmentResponse> findByTrip(@PathVariable @Positive Long id) {
        return service.findByTrip(id);
    }
}
