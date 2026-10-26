package com.smartshipment.trip.dto;

import java.time.Instant;
import java.util.List;

public record TripResponse(Long id, String origin, String destination,
        Instant departureAt, Instant expectedArrivalAt, Long version, boolean deleted,
        Long vehicleId, List<Long> employeeIds) {
}
