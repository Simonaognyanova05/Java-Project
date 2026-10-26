package com.smartshipment.shipment.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartshipment.shipment.dto.ShipmentRequest;
import com.smartshipment.shipment.dto.ShipmentResponse;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.mapper.ShipmentMapper;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.trip.entity.Trip;
import com.smartshipment.trip.repository.TripRepository;

@Service
@Transactional
public class ShipmentService {
	private final ShipmentRepository shipmentRepository;
	private final TripRepository tripRepository;
	private final ShipmentMapper shipmentMapper;

	public ShipmentService(ShipmentRepository shipmentRepository, TripRepository tripRepository,
			ShipmentMapper shipmentMapper) {
		this.shipmentRepository = shipmentRepository;
		this.tripRepository = tripRepository;
		this.shipmentMapper = shipmentMapper;
	}

	@Transactional(readOnly = true)
	public List<ShipmentResponse> findByTripId(Long tripId) {
		if (!tripRepository.existsById(tripId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found");
		}
		return shipmentRepository.findAllByTripId(tripId).stream().map(shipmentMapper::toResponse).toList();
	}

	public ShipmentResponse create(ShipmentRequest request) {
		if (shipmentRepository.existsByTrackingNumber(request.trackingNumber())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"A shipment with this tracking number already exists");
		}
		return shipmentMapper.toResponse(shipmentRepository.save(shipmentMapper.toEntity(request)));
	}

	public ShipmentResponse assignToTrip(Long shipmentId, Long tripId) {
		Shipment shipment = shipmentRepository.findById(shipmentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
		if (shipment.getTrip() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Shipment is already assigned to a trip");
		}
		Trip trip = tripRepository.findById(tripId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
		shipment.assignToTrip(trip);
		return shipmentMapper.toResponse(shipmentRepository.save(shipment));
	}
}
