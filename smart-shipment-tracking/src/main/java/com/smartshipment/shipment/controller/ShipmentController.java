package com.smartshipment.shipment.controller;

import java.net.URI;
import com.smartshipment.shipment.dto.ShipmentRequest;
import com.smartshipment.shipment.dto.ShipmentResponse;
import com.smartshipment.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
}
