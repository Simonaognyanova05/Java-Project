package com.smartshipment.tracking.entity;

import java.time.Instant;
import com.smartshipment.employee.entity.Employee;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "tracking_events")
public class TrackingEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false, updatable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, updatable = false)
    private ShipmentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ShipmentStatus newStatus;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", updatable = false)
    private Employee employee;

    protected TrackingEvent() {
    }

    public TrackingEvent(Shipment shipment, ShipmentStatus previousStatus, ShipmentStatus newStatus,
            Instant occurredAt, Employee employee) {
        this.shipment = shipment;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.occurredAt = occurredAt;
        this.employee = employee;
    }

    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public ShipmentStatus getPreviousStatus() { return previousStatus; }
    public ShipmentStatus getNewStatus() { return newStatus; }
    public Instant getOccurredAt() { return occurredAt; }
    public Employee getEmployee() { return employee; }
}
