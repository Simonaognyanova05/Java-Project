package com.smartshipment.shipment.controller;

import java.util.List;

import com.smartshipment.shipment.dto.ShipmentResponse;
import com.smartshipment.shipment.dto.ShipmentStatusRequest;
import com.smartshipment.shipment.service.ShipmentLifecycleService;
import com.smartshipment.tracking.dto.TrackingEventResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments/{id}")
public class ShipmentLifecycleController {
	private final ShipmentLifecycleService lifecycleService;

	public ShipmentLifecycleController(ShipmentLifecycleService lifecycleService) {
		this.lifecycleService = lifecycleService;
	}

	@PatchMapping("/status")
	public ShipmentResponse updateStatus(@PathVariable @Positive Long id,
			@Valid @RequestBody ShipmentStatusRequest request) {
		return lifecycleService.updateStatus(id, request);
	}

	@GetMapping("/history")
	public List<TrackingEventResponse> findHistory(@PathVariable @Positive Long id) {
		return lifecycleService.findHistory(id);
	}
}
