package com.smartshipment.shipment.service;

import java.time.Clock;
import java.time.Instant;

import com.smartshipment.customer.entity.Customer;
import com.smartshipment.customer.repository.CustomerRepository;
import com.smartshipment.shipment.dto.ShipmentRequest;
import com.smartshipment.shipment.dto.ShipmentResponse;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.shipment.mapper.ShipmentMapper;
import com.smartshipment.shipment.repository.ShipmentRepository;
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
public class ShipmentService {
    private final ShipmentRepository shipments;
    private final CustomerRepository customers;
    private final ShipmentMapper mapper;
    private final TrackingEventRepository events;
    private final Clock clock;

    public ShipmentService(ShipmentRepository shipments, CustomerRepository customers, ShipmentMapper mapper,
            TrackingEventRepository events, Clock clock) {
        this.shipments = shipments;
        this.customers = customers;
        this.mapper = mapper;
        this.events = events;
        this.clock = clock;
    }

    public ShipmentResponse create(@Valid ShipmentRequest request) {
        Customer customer = customers.findById(request.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        Shipment shipment = shipments.saveAndFlush(mapper.toEntity(request, customer));
        events.saveAndFlush(new TrackingEvent(shipment, null, ShipmentStatus.CREATED, Instant.now(clock), null));
        return mapper.toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse findById(@NotNull @Positive Long id) {
        return mapper.toResponse(shipments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found")));
    }
}
