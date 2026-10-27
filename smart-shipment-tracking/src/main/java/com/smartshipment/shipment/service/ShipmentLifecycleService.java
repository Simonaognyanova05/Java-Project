package com.smartshipment.shipment.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.smartshipment.employee.entity.Employee;
import com.smartshipment.employee.repository.EmployeeRepository;
import com.smartshipment.shipment.dto.ShipmentResponse;
import com.smartshipment.shipment.dto.ShipmentStatusRequest;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.shipment.mapper.ShipmentMapper;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.tracking.dto.TrackingEventResponse;
import com.smartshipment.tracking.entity.TrackingEvent;
import com.smartshipment.tracking.repository.TrackingEventRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ShipmentLifecycleService {
	private final ShipmentRepository shipments;
	private final EmployeeRepository employees;
	private final TrackingEventRepository events;
	private final ShipmentMapper mapper;
	private final Clock clock;

	public ShipmentLifecycleService(ShipmentRepository shipments, EmployeeRepository employees,
			TrackingEventRepository events, ShipmentMapper mapper, Clock clock) {
		this.shipments = shipments;
		this.employees = employees;
		this.events = events;
		this.mapper = mapper;
		this.clock = clock;
	}

	public ShipmentResponse updateStatus(@NotNull @Positive Long shipmentId,
			@Valid ShipmentStatusRequest request) {
		Shipment shipment = shipments.findForUpdate(shipmentId)
				.orElseThrow(() -> notFound("Shipment"));
		if (!Objects.equals(shipment.getVersion(), request.expectedVersion())) {
			throw conflict("Shipment was modified; reload it before changing its status");
		}
		if (request.status() == ShipmentStatus.LOADED) {
			throw conflict("Use the load endpoint to move an accepted shipment to LOADED");
		}

		Employee employee = employees.findByIdAndDeletedFalse(request.employeeId())
				.orElseThrow(() -> notFound("Active employee"));
		ShipmentStatus previousStatus = shipment.getStatus();
		try {
			shipment.transitionTo(request.status());
		} catch (IllegalStateException exception) {
			throw conflict(exception.getMessage());
		}

		events.saveAndFlush(new TrackingEvent(shipment, previousStatus, request.status(), Instant.now(clock), employee));
		shipments.flush();
		return mapper.toResponse(shipment);
	}

	@Transactional(readOnly = true)
	public List<TrackingEventResponse> findHistory(@NotNull @Positive Long shipmentId) {
		if (!shipments.existsById(shipmentId)) {
			throw notFound("Shipment");
		}
		return events.findAllByShipmentIdOrderByOccurredAtAscIdAsc(shipmentId).stream()
				.map(TrackingEventResponse::from).toList();
	}

	private ResponseStatusException notFound(String resource) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
	}

	private ResponseStatusException conflict(String message) {
		return new ResponseStatusException(HttpStatus.CONFLICT, message);
	}
}
