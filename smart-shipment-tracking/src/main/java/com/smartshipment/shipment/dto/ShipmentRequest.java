package com.smartshipment.shipment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ShipmentRequest(
		@NotBlank @Size(max = 50) String trackingNumber,
		@NotBlank @Size(max = 160) String recipientName,
		@NotBlank @Size(max = 255) String deliveryAddress,
		@NotNull @DecimalMin(value = "0.01") BigDecimal weightKg) {
}
