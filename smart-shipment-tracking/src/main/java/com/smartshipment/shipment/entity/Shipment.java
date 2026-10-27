package com.smartshipment.shipment.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.smartshipment.customer.entity.Customer;
import com.smartshipment.trip.entity.Trip;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "shipments")
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String trackingNumber = UUID.randomUUID().toString();

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant expectedDeliveryAt;

    @Version
    private Long version;

    @Column(length = 160)
    private String recipientName;

    @Column(length = 255)
    private String deliveryAddress;

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    protected Shipment() {
    }

    public Shipment(Customer customer, BigDecimal weightKg) {
        this.customer = customer;
        this.weightKg = weightKg;
    }

    public Long getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public Customer getCustomer() { return customer; }
    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public ShipmentStatus getStatus() { return status; }
    public BigDecimal getWeightKg() { return weightKg; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpectedDeliveryAt() { return expectedDeliveryAt; }
    public void setExpectedDeliveryAt(Instant expectedDeliveryAt) { this.expectedDeliveryAt = expectedDeliveryAt; }
    public void markLoaded() {
        transitionTo(ShipmentStatus.LOADED);
    }

    public void transitionTo(ShipmentStatus nextStatus) {
        boolean allowed = switch (status) {
            case CREATED -> nextStatus == ShipmentStatus.ACCEPTED;
            case ACCEPTED -> nextStatus == ShipmentStatus.LOADED;
            case LOADED -> nextStatus == ShipmentStatus.IN_TRANSIT;
            case IN_TRANSIT -> nextStatus == ShipmentStatus.DELIVERED;
            case DELIVERED -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Invalid shipment status transition: " + status + " -> " + nextStatus);
        }
        status = nextStatus;
    }

    public Long getVersion() { return version; }
}
