package com.smartshipment.vehicle.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles")
public class Vehicle {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String licensePlate;

	@Column(nullable = false, length = 80)
	private String make;

	@Column(nullable = false, length = 80)
	private String model;

	@Column(nullable = false, length = 40)
	private String vehicleType;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal capacityKg;

	protected Vehicle() {
	}

	public Vehicle(String licensePlate, String make, String model, String vehicleType, BigDecimal capacityKg) {
		this.licensePlate = licensePlate;
		this.make = make;
		this.model = model;
		this.vehicleType = vehicleType;
		this.capacityKg = capacityKg;
	}

	public Long getId() {
		return id;
	}

	public String getLicensePlate() {
		return licensePlate;
	}

	public void setLicensePlate(String licensePlate) {
		this.licensePlate = licensePlate;
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(String vehicleType) {
		this.vehicleType = vehicleType;
	}

	public BigDecimal getCapacityKg() {
		return capacityKg;
	}

	public void setCapacityKg(BigDecimal capacityKg) {
		this.capacityKg = capacityKg;
	}
}
