package com.smartshipment.shipment.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.smartshipment.shipment.dto.OverdueShipmentResponse;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.shipment.repository.ShipmentRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class OverdueShipmentService {
    private final ShipmentRepository shipments;
    private final Clock clock;

    public OverdueShipmentService(ShipmentRepository shipments, Clock clock) {
        this.shipments = shipments;
        this.clock = clock;
    }

    public List<OverdueShipmentResponse> findOverdue(@Min(0) int page, @Min(1) @Max(100) int size) {
        return shipments.findOverdueShipments(Instant.now(clock), ShipmentStatus.DELIVERED,
                        PageRequest.of(page, size)).stream()
                .map(OverdueShipmentResponse::from).toList();
    }
}
