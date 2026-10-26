package com.smartshipment.trip.dto;

import java.time.LocalDateTime;

import com.smartshipment.trip.entity.TripStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TripRequest(
		@NotBlank @Size(max = 50) String trackingNumber,
		@NotBlank @Size(max = 120) String origin,
		@NotBlank @Size(max = 120) String destination,
		@NotNull TripStatus status,
		@NotNull LocalDateTime departureDate,
		@NotNull LocalDateTime estimatedArrivalDate,
		LocalDateTime actualArrivalDate) {
}
