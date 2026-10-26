package com.smartshipment.trip.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record TripRequest(
        @NotBlank @Size(max = 255) String origin,
        @NotBlank @Size(max = 255) String destination,
        @NotNull Instant departureAt,
        @NotNull Instant expectedArrivalAt, @NotNull @Positive Long vehicleId) {

    @JsonIgnore
    @AssertTrue(message = "Expected arrival must be after departure")
    public boolean isScheduleValid() {
        return departureAt == null || expectedArrivalAt == null
                || departureAt.isBefore(expectedArrivalAt);
    }
}
