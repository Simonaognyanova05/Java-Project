package com.smartshipment.shipment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.smartshipment.customer.entity.Customer;
import com.smartshipment.customer.repository.CustomerRepository;
import com.smartshipment.employee.entity.Employee;
import com.smartshipment.employee.repository.EmployeeRepository;
import com.smartshipment.shipment.dto.ShipmentStatusRequest;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.shipment.service.ShipmentLifecycleService;
import com.smartshipment.tracking.entity.TrackingEvent;
import com.smartshipment.tracking.repository.TrackingEventRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class ShipmentStatusConcurrencyIntegrationTests {
	private static final String SCHEMA = "status_concurrency_" + UUID.randomUUID().toString().replace("-", "");

	@DynamicPropertySource
	static void isolatedSchema(DynamicPropertyRegistry registry) {
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
		registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
		registry.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");
	}

	@Autowired private ShipmentLifecycleService lifecycle;
	@Autowired private ShipmentRepository shipments;
	@Autowired private CustomerRepository customers;
	@Autowired private EmployeeRepository employees;
	@Autowired private TrackingEventRepository events;
	@Autowired private PlatformTransactionManager transactionManager;

	private record Fixture(long shipmentId, long employeeA, long employeeB) { }

	private TransactionTemplate transaction() {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		transaction.setTimeout(20);
		return transaction;
	}

	private Fixture fixture() {
		return transaction().execute(status -> {
			String suffix = UUID.randomUUID().toString().substring(0, 12);
			Employee first = employees.saveAndFlush(new Employee("Ivan", "Petrov", "STATUS-A-" + suffix));
			Employee second = employees.saveAndFlush(new Employee("Maria", "Petrova", "STATUS-B-" + suffix));
			Customer customer = customers.saveAndFlush(
					new Customer("Test", "Customer", suffix + "@example.com", null));
			Shipment shipment = shipments.saveAndFlush(new Shipment(customer, BigDecimal.TEN));
			events.saveAndFlush(new TrackingEvent(shipment, null, ShipmentStatus.CREATED, Instant.now(), null));
			return new Fixture(shipment.getId(), first.getId(), second.getId());
		});
	}

	private long changeStatus(long shipmentId, long employeeId, CyclicBarrier barrier) {
		try {
			return transaction().execute(status -> {
				try {
					barrier.await(10, TimeUnit.SECONDS);
				} catch (Exception exception) {
					throw new IllegalStateException("Concurrent status requests did not synchronize", exception);
				}
				lifecycle.updateStatus(shipmentId,
						new ShipmentStatusRequest(ShipmentStatus.ACCEPTED, employeeId, 0L));
				return employeeId;
			});
		} catch (ResponseStatusException exception) {
			assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(exception.getReason())
					.isEqualTo("Shipment was modified; reload it before changing its status");
			return 0L;
		}
	}

	@Test
	void concurrentStatusChangesWithSameVersionAllowOnlyOneWinner() throws Exception {
		Fixture fixture = fixture();
		CyclicBarrier barrier = new CyclicBarrier(2);
		List<Long> results;
		try (var executor = Executors.newFixedThreadPool(2)) {
			Future<Long> first = executor.submit(
					() -> changeStatus(fixture.shipmentId(), fixture.employeeA(), barrier));
			Future<Long> second = executor.submit(
					() -> changeStatus(fixture.shipmentId(), fixture.employeeB(), barrier));
			results = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
		}

		assertThat(results.stream().filter(result -> result == 0L).count()).isEqualTo(1);
		long winner = results.stream().filter(result -> result != 0L).findFirst().orElseThrow();
		transaction().executeWithoutResult(status -> {
			Shipment shipment = shipments.findById(fixture.shipmentId()).orElseThrow();
			assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.ACCEPTED);
			assertThat(shipment.getVersion()).isEqualTo(1L);

			List<TrackingEvent> history = events
					.findAllByShipmentIdOrderByOccurredAtAscIdAsc(fixture.shipmentId());
			assertThat(history).hasSize(2);
			assertThat(history.getFirst().getNewStatus()).isEqualTo(ShipmentStatus.CREATED);
			assertThat(history.getLast().getPreviousStatus()).isEqualTo(ShipmentStatus.CREATED);
			assertThat(history.getLast().getNewStatus()).isEqualTo(ShipmentStatus.ACCEPTED);
			assertThat(history.getLast().getEmployee().getId()).isEqualTo(winner);
		});
	}
}
