package com.smartshipment.shipment.dto;

import com.smartshipment.shipment.entity.ShipmentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ShipmentStatusRequest(
		@NotNull ShipmentStatus status,
		@NotNull @Positive Long employeeId,
		@NotNull @PositiveOrZero Long expectedVersion) {
}
