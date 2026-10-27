package com.smartshipment.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.smartshipment.customer.entity.Customer;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;

class ShipmentStatusTransitionTests {
	@Test
	void shipmentCanMoveThroughEveryAllowedStatus() {
		Shipment shipment = newShipment();

		assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CREATED);

		shipment.transitionTo(ShipmentStatus.ACCEPTED);
		assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.ACCEPTED);

		shipment.markLoaded();
		assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.LOADED);

		shipment.transitionTo(ShipmentStatus.IN_TRANSIT);
		assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);

		shipment.transitionTo(ShipmentStatus.DELIVERED);
		assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
	}

	@ParameterizedTest
	@MethodSource("forbiddenTransitions")
	void shipmentRejectsForbiddenStatusTransitions(ShipmentStatus currentStatus, ShipmentStatus nextStatus) {
		Shipment shipment = newShipmentAt(currentStatus);

		assertThatThrownBy(() -> shipment.transitionTo(nextStatus))
				.isInstanceOf(IllegalStateException.class);
		assertThat(shipment.getStatus()).isEqualTo(currentStatus);
	}

	@Test
	void shipmentCannotBeLoadedBeforeItIsAccepted() {
		Shipment shipment = newShipment();

		assertThatThrownBy(shipment::markLoaded).isInstanceOf(IllegalStateException.class);
		assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CREATED);
	}

	private static Stream<Arguments> forbiddenTransitions() {
		return Stream.of(
				Arguments.of(ShipmentStatus.CREATED, ShipmentStatus.LOADED),
				Arguments.of(ShipmentStatus.CREATED, ShipmentStatus.DELIVERED),
				Arguments.of(ShipmentStatus.ACCEPTED, ShipmentStatus.IN_TRANSIT),
				Arguments.of(ShipmentStatus.LOADED, ShipmentStatus.ACCEPTED),
				Arguments.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.LOADED),
				Arguments.of(ShipmentStatus.DELIVERED, ShipmentStatus.ACCEPTED),
				Arguments.of(ShipmentStatus.DELIVERED, ShipmentStatus.DELIVERED));
	}

	private static Shipment newShipmentAt(ShipmentStatus status) {
		Shipment shipment = newShipment();
		switch (status) {
			case CREATED -> { }
			case ACCEPTED -> shipment.transitionTo(ShipmentStatus.ACCEPTED);
			case LOADED -> {
				shipment.transitionTo(ShipmentStatus.ACCEPTED);
				shipment.markLoaded();
			}
			case IN_TRANSIT -> {
				shipment.transitionTo(ShipmentStatus.ACCEPTED);
				shipment.markLoaded();
				shipment.transitionTo(ShipmentStatus.IN_TRANSIT);
			}
			case DELIVERED -> {
				shipment.transitionTo(ShipmentStatus.ACCEPTED);
				shipment.markLoaded();
				shipment.transitionTo(ShipmentStatus.IN_TRANSIT);
				shipment.transitionTo(ShipmentStatus.DELIVERED);
			}
		}
		return shipment;
	}

	private static Shipment newShipment() {
		Customer customer = new Customer("Test", "Customer", "test@example.com", null);
		return new Shipment(customer, BigDecimal.ONE);
	}
}
