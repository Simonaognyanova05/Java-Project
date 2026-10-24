package com.smartshipment.trip.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String origin;

    @Column(nullable = false, length = 255)
    private String destination;

    @Column(nullable = false)
    private Instant departureAt;

    @Column(nullable = false)
    private Instant expectedArrivalAt;

    @Version
    private Long version;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    protected Trip() {
    }

    public Trip(String origin, String destination, Instant departureAt, Instant expectedArrivalAt) {
        this.origin = origin;
        this.destination = destination;
        this.departureAt = departureAt;
        this.expectedArrivalAt = expectedArrivalAt;
    }

    public Long getId() { return id; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public Instant getDepartureAt() { return departureAt; }
    public void setDepartureAt(Instant departureAt) { this.departureAt = departureAt; }
    public Instant getExpectedArrivalAt() { return expectedArrivalAt; }
    public void setExpectedArrivalAt(Instant expectedArrivalAt) { this.expectedArrivalAt = expectedArrivalAt; }
    public Long getVersion() { return version; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
