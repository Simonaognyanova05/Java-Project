package com.smartshipment.shipment;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
import com.smartshipment.shipment.dto.AssignTripRequest;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.shipment.service.ShipmentAssignmentService;
import com.smartshipment.shipment.service.ShipmentLoadingService;
import com.smartshipment.tracking.entity.TrackingEvent;
import com.smartshipment.tracking.repository.TrackingEventRepository;
import com.smartshipment.trip.entity.Trip;
import com.smartshipment.trip.repository.TripRepository;
import com.smartshipment.vehicle.entity.Vehicle;
import com.smartshipment.vehicle.repository.VehicleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class ShipmentLoadingIntegrationTests {
    private static final String SCHEMA = "loading_test_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void isolatedSchema(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");
    }

    @Autowired private ShipmentLoadingService loading;
    @Autowired private ShipmentAssignmentService assignments;
    @Autowired private ShipmentRepository shipments;
    @Autowired private CustomerRepository customers;
    @Autowired private EmployeeRepository employees;
    @Autowired private VehicleRepository vehicles;
    @Autowired private TripRepository trips;
    @Autowired private TrackingEventRepository events;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    private record Fixture(long tripId, long vehicleId, List<Long> shipmentIds, long employeeA, long employeeB) { }

    private TransactionTemplate transaction() {
        TransactionTemplate result = new TransactionTemplate(transactionManager);
        result.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        result.setTimeout(20);
        return result;
    }

    private Fixture fixture(String capacity, String... weights) {
        return transaction().execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 12);
            Employee first = employees.saveAndFlush(new Employee("Ivan", "Petrov", "A-" + suffix));
            Employee second = employees.saveAndFlush(new Employee("Maria", "Petrova", "B-" + suffix));
            Vehicle vehicle = vehicles.saveAndFlush(new Vehicle("REG-" + suffix, new BigDecimal(capacity)));
            Trip trip = new Trip("Sofia", "Vienna", Instant.parse("2027-01-01T08:00:00Z"),
                    Instant.parse("2027-01-02T08:00:00Z"));
            trip.setVehicle(vehicle);
            trips.saveAndFlush(trip);
            Customer customer = customers.saveAndFlush(new Customer("Test", "Customer", suffix + "@example.com", null));
            List<Long> ids = new ArrayList<>();
            for (String weight : weights) {
                Shipment shipment = shipments.saveAndFlush(new Shipment(customer, new BigDecimal(weight)));
                assignments.assign(shipment.getId(), new AssignTripRequest(trip.getId(), 0L));
                // Committed ACCEPTED fixtures: acceptance workflow belongs to the lifecycle module.
                entityManager.createQuery("update Shipment s set s.status = :accepted where s.id = :id")
                        .setParameter("accepted", ShipmentStatus.ACCEPTED).setParameter("id", shipment.getId())
                        .executeUpdate();
                ids.add(shipment.getId());
            }
            entityManager.clear();
            return new Fixture(trip.getId(), vehicle.getId(), ids, first.getId(), second.getId());
        });
    }

    private long attempt(long shipmentId, long employeeId, CyclicBarrier barrier) {
        try {
            transaction().executeWithoutResult(status -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException("Concurrent loading did not synchronize", exception);
                }
                assertThat(loading.load(shipmentId, employeeId, 1L).status()).isEqualTo(ShipmentStatus.LOADED);
            });
            return employeeId;
        } catch (ResponseStatusException exception) {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.getReason()).isIn("Shipment was modified; reload it before loading",
                    "Trip capacity is exceeded");
            return 0L;
        }
    }

    private List<Long> compete(Fixture fixture, long firstShipmentId, long secondShipmentId) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Long> first = executor.submit(() -> attempt(firstShipmentId, fixture.employeeA(), barrier));
            Future<Long> second = executor.submit(() -> attempt(secondShipmentId, fixture.employeeB(), barrier));
            return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        }
    }

    @Test
    void sameShipmentCanBeLoadedOnlyOnceAndHasExactlyOneEmployeeEvent() throws Exception {
        Fixture fixture = fixture("100.00", "60.00");
        long shipmentId = fixture.shipmentIds().getFirst();
        List<Long> results = compete(fixture, shipmentId, shipmentId);
        assertThat(results.stream().filter(id -> id == 0L).count()).isEqualTo(1);
        long winner = results.stream().filter(id -> id != 0L).findFirst().orElseThrow();
        transaction().executeWithoutResult(status -> {
            Shipment shipment = shipments.findById(shipmentId).orElseThrow();
            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.LOADED);
            assertThat(shipment.getVersion()).isEqualTo(2L);
            List<TrackingEvent> history = events.findAllByShipmentIdOrderByOccurredAtAscIdAsc(shipmentId);
            assertThat(history).hasSize(1);
            assertThat(history.getFirst().getPreviousStatus()).isEqualTo(ShipmentStatus.ACCEPTED);
            assertThat(history.getFirst().getNewStatus()).isEqualTo(ShipmentStatus.LOADED);
            assertThat(history.getFirst().getEmployee().getId()).isEqualTo(winner);
            assertThat(history.getFirst().getOccurredAt()).isNotNull();
        });
    }

    @Test
    void differentShipmentsCanLoadConcurrentlyAtExactlyReservedCapacity() throws Exception {
        Fixture fixture = fixture("100.00", "60.00", "40.00");
        assertThat(compete(fixture, fixture.shipmentIds().get(0), fixture.shipmentIds().get(1)))
                .containsExactly(fixture.employeeA(), fixture.employeeB());
        transaction().executeWithoutResult(status -> {
            assertThat(shipments.totalAssignedWeight(fixture.tripId())).isEqualByComparingTo("100.00");
            for (Long id : fixture.shipmentIds()) {
                assertThat(shipments.findById(id).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.LOADED);
                assertThat(events.findAllByShipmentIdOrderByOccurredAtAscIdAsc(id)).hasSize(1);
            }
        });
    }

    @Test
    void loadingRechecksCapacityAndRejectsAnOversubscribedLegacyTrip() throws Exception {
        Fixture fixture = fixture("200.00", "60.00", "60.00");
        // Deliberately invalid legacy state; normal assignment never permits this.
        transaction().executeWithoutResult(status -> {
            vehicles.findById(fixture.vehicleId()).orElseThrow().setCapacityKg(new BigDecimal("100.00"));
        });
        assertThat(compete(fixture, fixture.shipmentIds().get(0), fixture.shipmentIds().get(1)))
                .containsExactly(0L, 0L);
        transaction().executeWithoutResult(status -> {
            for (Long id : fixture.shipmentIds()) {
                Shipment shipment = shipments.findById(id).orElseThrow();
                assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.ACCEPTED);
                assertThat(shipment.getVersion()).isEqualTo(1L);
                assertThat(events.findAllByShipmentIdOrderByOccurredAtAscIdAsc(id)).isEmpty();
            }
        });
    }

    @Test
    void failureToPersistHistoryRollsBackShipmentStatusAndVersion() {
        Fixture fixture = fixture("100.00", "60.00");
        long shipmentId = fixture.shipmentIds().getFirst();
        // Fail the actual history INSERT in this test's isolated schema, without mocks.
        jdbc.execute("ALTER TABLE " + SCHEMA + ".tracking_events ADD CONSTRAINT reject_loading_event CHECK (new_status <> 'LOADED' OR shipment_id <> " + shipmentId + ")");
        try {
            assertThatThrownBy(() -> loading.load(shipmentId, fixture.employeeA(), 1L))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbc.execute("ALTER TABLE " + SCHEMA + ".tracking_events DROP CONSTRAINT reject_loading_event");
        }
        transaction().executeWithoutResult(status -> {
            Shipment shipment = shipments.findById(shipmentId).orElseThrow();
            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.ACCEPTED);
            assertThat(shipment.getVersion()).isEqualTo(1L);
            assertThat(events.findAllByShipmentIdOrderByOccurredAtAscIdAsc(shipmentId)).isEmpty();
        });
    }
}