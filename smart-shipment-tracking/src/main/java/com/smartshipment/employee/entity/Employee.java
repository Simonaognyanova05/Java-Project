package com.smartshipment.employee.entity;

import java.util.HashSet;
import java.util.Set;

import com.smartshipment.trip.entity.Trip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
public class Employee {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 80)
	private String firstName;

	@Column(nullable = false, length = 80)
	private String lastName;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(length = 30)
	private String phone;

	@ManyToMany(mappedBy = "employees", fetch = FetchType.LAZY)
	private Set<Trip> trips = new HashSet<>();

	protected Employee() {
	}

	public Employee(String firstName, String lastName, String email, String phone) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phone = phone;
	}

	public Long getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Set<Trip> getTrips() {
		return trips;
	}

	public void addTrip(Trip trip) {
		trips.add(trip);
		trip.getEmployees().add(this);
	}

	public void removeTrip(Trip trip) {
		trips.remove(trip);
		trip.getEmployees().remove(this);
	}
}
