package com.smartshipment.trip.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshipment.trip.entity.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {
	boolean existsByTrackingNumber(String trackingNumber);

	boolean existsByTrackingNumberAndIdNot(String trackingNumber, Long id);
}
