package com.smartshipment.trip.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.smartshipment.employee.entity.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "trips")
public class Trip {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String trackingNumber;

	@Column(nullable = false, length = 120)
	private String origin;

	@Column(nullable = false, length = 120)
	private String destination;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TripStatus status;

	@Column(nullable = false)
	private LocalDateTime departureDate;

	@Column(nullable = false)
	private LocalDateTime estimatedArrivalDate;

	private LocalDateTime actualArrivalDate;

	@ManyToMany
	@JoinTable(name = "trip_employees",
			joinColumns = @JoinColumn(name = "trip_id"),
			inverseJoinColumns = @JoinColumn(name = "employee_id"))
	private Set<Employee> employees = new HashSet<>();

	protected Trip() {
	}

	public Trip(String trackingNumber, String origin, String destination, TripStatus status,
			LocalDateTime departureDate, LocalDateTime estimatedArrivalDate, LocalDateTime actualArrivalDate) {
		this.trackingNumber = trackingNumber;
		this.origin = origin;
		this.destination = destination;
		this.status = status;
		this.departureDate = departureDate;
		this.estimatedArrivalDate = estimatedArrivalDate;
		this.actualArrivalDate = actualArrivalDate;
	}

	public Long getId() {
		return id;
	}

	public String getTrackingNumber() {
		return trackingNumber;
	}

	public void setTrackingNumber(String trackingNumber) {
		this.trackingNumber = trackingNumber;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public TripStatus getStatus() {
		return status;
	}

	public void setStatus(TripStatus status) {
		this.status = status;
	}

	public LocalDateTime getDepartureDate() {
		return departureDate;
	}

	public void setDepartureDate(LocalDateTime departureDate) {
		this.departureDate = departureDate;
	}

	public LocalDateTime getEstimatedArrivalDate() {
		return estimatedArrivalDate;
	}

	public void setEstimatedArrivalDate(LocalDateTime estimatedArrivalDate) {
		this.estimatedArrivalDate = estimatedArrivalDate;
	}

	public LocalDateTime getActualArrivalDate() {
		return actualArrivalDate;
	}

	public void setActualArrivalDate(LocalDateTime actualArrivalDate) {
		this.actualArrivalDate = actualArrivalDate;
	}

	public Set<Employee> getEmployees() {
		return employees;
	}

	public void addEmployee(Employee employee) {
		employees.add(employee);
		employee.getTrips().add(this);
	}

	public void removeEmployee(Employee employee) {
		employees.remove(employee);
		employee.getTrips().remove(this);
	}
}
