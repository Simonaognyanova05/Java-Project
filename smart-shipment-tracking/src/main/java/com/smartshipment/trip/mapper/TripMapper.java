package com.smartshipment.trip.mapper;

import org.springframework.stereotype.Component;

import com.smartshipment.trip.dto.TripRequest;
import com.smartshipment.trip.dto.TripResponse;
import com.smartshipment.trip.entity.Trip;

@Component
public class TripMapper {
	public Trip toEntity(TripRequest request) {
		return new Trip(request.trackingNumber(), request.origin(), request.destination(), request.status(),
				request.departureDate(), request.estimatedArrivalDate(), request.actualArrivalDate());
	}

	public void updateEntity(Trip trip, TripRequest request) {
		trip.setTrackingNumber(request.trackingNumber());
		trip.setOrigin(request.origin());
		trip.setDestination(request.destination());
		trip.setStatus(request.status());
		trip.setDepartureDate(request.departureDate());
		trip.setEstimatedArrivalDate(request.estimatedArrivalDate());
		trip.setActualArrivalDate(request.actualArrivalDate());
	}

	public TripResponse toResponse(Trip trip) {
		return new TripResponse(trip.getId(), trip.getTrackingNumber(), trip.getOrigin(), trip.getDestination(),
				trip.getStatus(), trip.getDepartureDate(), trip.getEstimatedArrivalDate(), trip.getActualArrivalDate());
	}
}
