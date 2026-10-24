package com.smartshipment.trip.mapper;

import org.springframework.stereotype.Component;

import com.smartshipment.trip.dto.TripRequest;
import com.smartshipment.trip.dto.TripResponse;
import com.smartshipment.trip.entity.Trip;

@Component
public class TripMapper {
    public Trip toEntity(TripRequest request) {
        return new Trip(request.origin(), request.destination(), request.departureAt(), request.expectedArrivalAt());
    }

    public void updateEntity(Trip trip, TripRequest request) {
        trip.setOrigin(request.origin());
        trip.setDestination(request.destination());
        trip.setDepartureAt(request.departureAt());
        trip.setExpectedArrivalAt(request.expectedArrivalAt());
    }

    public TripResponse toResponse(Trip trip) {
        return new TripResponse(trip.getId(), trip.getOrigin(), trip.getDestination(),
                trip.getDepartureAt(), trip.getExpectedArrivalAt(), trip.getVersion(), trip.isDeleted(), trip.getVehicle() == null ? null : trip.getVehicle().getId(), trip.getEmployees().stream().map(employee -> employee.getId()).sorted().toList());
    }
}
