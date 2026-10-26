package com.smartshipment.shipment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartshipment.shipment.entity.Shipment;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
	boolean existsByTrackingNumber(String trackingNumber);

	List<Shipment> findAllByTripId(Long tripId);
}
