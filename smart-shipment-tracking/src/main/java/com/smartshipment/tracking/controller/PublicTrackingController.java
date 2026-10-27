package com.smartshipment.tracking.controller;

import com.smartshipment.tracking.dto.PublicTrackingResponse;
import com.smartshipment.tracking.service.PublicTrackingService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/tracking")
public class PublicTrackingController {
	private final PublicTrackingService publicTrackingService;

	public PublicTrackingController(PublicTrackingService publicTrackingService) {
		this.publicTrackingService = publicTrackingService;
	}

	@GetMapping("/{trackingNumber}")
	public PublicTrackingResponse findByTrackingNumber(
			@PathVariable @NotBlank @Size(max = 36) String trackingNumber) {
		return publicTrackingService.findByTrackingNumber(trackingNumber);
	}
}
