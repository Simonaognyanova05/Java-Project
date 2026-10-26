package com.smartshipment.shipment.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartshipment.shipment.dto.ShipmentRequest;
import com.smartshipment.shipment.dto.ShipmentResponse;
import com.smartshipment.shipment.service.ShipmentService;

import jakarta.validation.Valid;

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

	@PutMapping("/{shipmentId}/trip/{tripId}")
	public ShipmentResponse assignToTrip(@PathVariable Long shipmentId, @PathVariable Long tripId) {
		return shipmentService.assignToTrip(shipmentId, tripId);
	}

	@GetMapping("/trip/{tripId}")
	public List<ShipmentResponse> findByTripId(@PathVariable Long tripId) {
		return shipmentService.findByTripId(tripId);
	}
}
