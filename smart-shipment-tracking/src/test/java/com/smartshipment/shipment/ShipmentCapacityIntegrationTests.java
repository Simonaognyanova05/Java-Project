package com.smartshipment.shipment;

import static org.assertj.core.api.Assertions.*;

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
import com.smartshipment.shipment.dto.AssignTripRequest;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.shipment.service.ShipmentAssignmentService;
import com.smartshipment.trip.entity.Trip;
import com.smartshipment.trip.repository.TripRepository;
import com.smartshipment.vehicle.entity.Vehicle;
import com.smartshipment.vehicle.repository.VehicleRepository;
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
class ShipmentCapacityIntegrationTests {
    // A new schema for this class on both H2 and PostgreSQL. Never use public.
    private static final String SCHEMA = "capacity_test_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void isolatedSchema(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");
    }

    @Autowired private ShipmentAssignmentService assignments;
    @Autowired private ShipmentRepository shipments;
    @Autowired private TripRepository trips;
    @Autowired private VehicleRepository vehicles;
    @Autowired private CustomerRepository customers;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setTimeout(20);
        return transaction;
    }

    private Trip trip(String capacity) {
        Vehicle vehicle = vehicles.saveAndFlush(new Vehicle(UUID.randomUUID().toString().substring(0, 20),
                new BigDecimal(capacity)));
        Trip trip = new Trip("Sofia", "Vienna", Instant.parse("2027-01-01T08:00:00Z"),
                Instant.parse("2027-01-02T08:00:00Z"));
        trip.setVehicle(vehicle);
        return trips.saveAndFlush(trip);
    }

    private Shipment shipment(String weight) {
        Customer customer = customers.saveAndFlush(new Customer("Ivan", "Petrov",
                UUID.randomUUID() + "@example.com", null));
        return shipments.saveAndFlush(new Shipment(customer, new BigDecimal(weight)));
    }

    @Test
    void exactCapacityIsAllowedAndExcessAssignmentRollsBack() {
        long[] ids = transaction().execute(status -> {
            Trip target = trip("100.00");
            Shipment first = shipment("33.33");
            Shipment second = shipment("66.67");
            Shipment excess = shipment("0.01");
            return new long[] {target.getId(), first.getId(), second.getId(), excess.getId()};
        });
        assignments.assign(ids[1], new AssignTripRequest(ids[0], 0L));
        assignments.assign(ids[2], new AssignTripRequest(ids[0], 0L));
        assertThatThrownBy(() -> assignments.assign(ids[3], new AssignTripRequest(ids[0], 0L)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        transaction().executeWithoutResult(status -> {
            assertThat(shipments.totalAssignedWeight(ids[0])).isEqualByComparingTo("100.00");
            Shipment rejected = shipments.findById(ids[3]).orElseThrow();
            assertThat(rejected.getTrip()).isNull();
            assertThat(rejected.getVersion()).isZero();
        });
    }

    @Test
    void competingReassignmentsCannotExceedCapacityAndLoserKeepsOldTrip() throws Exception {
        long[] ids = transaction().execute(status -> {
            Trip source = trip("200.00");
            Trip otherSource = trip("200.00");
            Trip target = trip("100.00");
            Shipment reserved = shipment("20.00");
            Shipment first = shipment("60.00");
            Shipment second = shipment("60.00");
            assignments.assign(reserved.getId(), new AssignTripRequest(target.getId(), 0L));
            assignments.assign(first.getId(), new AssignTripRequest(source.getId(), 0L));
            assignments.assign(second.getId(), new AssignTripRequest(otherSource.getId(), 0L));
            return new long[] {source.getId(), target.getId(), first.getId(), second.getId(), otherSource.getId()};
        });
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> compete(ids[2], ids[1], barrier));
            Future<Boolean> second = executor.submit(() -> compete(ids[3], ids[1], barrier));
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        transaction().executeWithoutResult(status -> {
            assertThat(shipments.totalAssignedWeight(ids[1])).isEqualByComparingTo("80.00");
            assertThat(shipments.totalAssignedWeight(ids[0]).add(shipments.totalAssignedWeight(ids[4]))).isEqualByComparingTo("60.00");
            Shipment first = shipments.findById(ids[2]).orElseThrow();
            Shipment second = shipments.findById(ids[3]).orElseThrow();
            assertThat(first.getTrip().getId()).isIn(ids[0], ids[1]);
            assertThat(second.getTrip().getId()).isIn(ids[4], ids[1]);
            assertThat(List.of(first.getVersion(), second.getVersion())).containsExactlyInAnyOrder(1L, 2L);
        });
    }

    private boolean compete(long shipmentId, long targetTripId, CyclicBarrier barrier) {
        try {
            transaction().executeWithoutResult(status -> {
                // Each worker owns a distinct shipment before either attempts the trip locks.
                shipments.findForUpdate(shipmentId).orElseThrow();
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException("Workers failed to synchronize", exception);
                }
                assignments.assign(shipmentId, new AssignTripRequest(targetTripId, 1L));
            });
            return true;
        } catch (ResponseStatusException exception) {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.getReason()).isEqualTo("Trip capacity would be exceeded");
            return false;
        }
    }
}