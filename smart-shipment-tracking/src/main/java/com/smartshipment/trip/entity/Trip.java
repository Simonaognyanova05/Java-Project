package com.smartshipment.trip.entity;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.smartshipment.employee.entity.Employee;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.UniqueConstraint;

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

    @ManyToMany
    @JoinTable(name = "trip_employees",
            joinColumns = @JoinColumn(name = "trip_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "employee_id"}))
    private Set<Employee> employees = new HashSet<>();

    public Set<Employee> getEmployees() {
        return Collections.unmodifiableSet(employees);
    }

    public void addEmployee(Employee employee) {
        employees.add(Objects.requireNonNull(employee, "Employee is required"));
    }

    public void removeEmployee(Employee employee) {
        employees.remove(employee);
    }

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
