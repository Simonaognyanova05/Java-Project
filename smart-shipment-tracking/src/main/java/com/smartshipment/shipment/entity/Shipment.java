package com.smartshipment.shipment.entity;

import java.math.BigDecimal;

import com.smartshipment.trip.entity.Trip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shipments")
public class Shipment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String trackingNumber;

	@Column(nullable = false, length = 160)
	private String recipientName;

	@Column(nullable = false, length = 255)
	private String deliveryAddress;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal weightKg;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trip_id")
	private Trip trip;

	protected Shipment() {
	}

	public Shipment(String trackingNumber, String recipientName, String deliveryAddress, BigDecimal weightKg) {
		this.trackingNumber = trackingNumber;
		this.recipientName = recipientName;
		this.deliveryAddress = deliveryAddress;
		this.weightKg = weightKg;
	}

	public Long getId() {
		return id;
	}

	public String getTrackingNumber() {
		return trackingNumber;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getDeliveryAddress() {
		return deliveryAddress;
	}

	public BigDecimal getWeightKg() {
		return weightKg;
	}

	public Trip getTrip() {
		return trip;
	}

	public void assignToTrip(Trip trip) {
		this.trip = trip;
	}
}
