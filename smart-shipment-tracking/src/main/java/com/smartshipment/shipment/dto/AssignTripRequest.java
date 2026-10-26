package com.smartshipment.shipment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record AssignTripRequest(@NotNull @Positive Long tripId,
        @NotNull @PositiveOrZero Long expectedVersion) {
}
