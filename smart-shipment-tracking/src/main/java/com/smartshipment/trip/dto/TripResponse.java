package com.smartshipment.trip.dto;

import java.time.LocalDateTime;

import com.smartshipment.trip.entity.TripStatus;

public record TripResponse(
		Long id,
		String trackingNumber,
		String origin,
		String destination,
		TripStatus status,
		LocalDateTime departureDate,
		LocalDateTime estimatedArrivalDate,
		LocalDateTime actualArrivalDate) {
}
