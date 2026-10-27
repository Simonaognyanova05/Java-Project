package com.smartshipment.shipment.controller;

import com.smartshipment.shipment.service.ShipmentQrCodeService;

import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentQrCodeController {
	private final ShipmentQrCodeService qrCodeService;

	public ShipmentQrCodeController(ShipmentQrCodeService qrCodeService) {
		this.qrCodeService = qrCodeService;
	}

	@GetMapping(value = "/{id}/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getQrCode(@PathVariable @Positive Long id) {
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.cacheControl(CacheControl.noCache())
				.body(qrCodeService.generateForShipment(id));
	}
}
