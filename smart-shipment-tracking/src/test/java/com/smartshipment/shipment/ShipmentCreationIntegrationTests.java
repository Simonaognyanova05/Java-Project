package com.smartshipment.shipment;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.smartshipment.customer.entity.Customer;
import com.smartshipment.customer.repository.CustomerRepository;
import com.smartshipment.employee.entity.Employee;
import com.smartshipment.employee.repository.EmployeeRepository;
import com.smartshipment.location.entity.Location;
import com.smartshipment.shipment.dto.ShipmentRequest;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.shipment.service.ShipmentService;
import com.smartshipment.tracking.repository.TrackingEventRepository;
import com.smartshipment.trip.entity.Trip;
import com.smartshipment.trip.repository.TripRepository;
import com.smartshipment.vehicle.entity.Vehicle;
import com.smartshipment.vehicle.repository.VehicleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class ShipmentCreationIntegrationTests {
    private static final String SCHEMA = "pr4_test_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void isolatedSchema(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private CustomerRepository customers;
    @Autowired private ShipmentRepository shipments;
    @Autowired private ShipmentService service;
    @Autowired private TrackingEventRepository events;
    @Autowired private VehicleRepository vehicles;
    @Autowired private EmployeeRepository employees;
    @Autowired private TripRepository trips;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    private long customer() {
        return customers.saveAndFlush(new Customer("Ivan", "Petrov", UUID.randomUUID() + "@example.com", null)).getId();
    }

    private String body(long customerId) {
        return """
                {"customerId":%d,"recipientName":"Maria Petrova","deliveryAddress":"Vienna 1",
                 "weightKg":60.00,"expectedDeliveryAt":"2030-01-01T12:00:00Z"}
                """.formatted(customerId);
    }

    private String create(long customerId) throws Exception {
        return mockMvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON).content(body(customerId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.recipientName").value("Maria Petrova"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getHeader("Location");
    }

    private long id(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void creationGeneratesUniqueTrackingAndOneInitialEventAndCanBeRead() throws Exception {
        long customerId = customer();
        String first = create(customerId);
        String second = create(customerId);
        mockMvc.perform(get(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryAddress").value("Vienna 1"))
                .andExpect(jsonPath("$.expectedDeliveryAt").value("2030-01-01T12:00:00Z"));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var firstShipment = shipments.findById(id(first)).orElseThrow();
            var secondShipment = shipments.findById(id(second)).orElseThrow();
            assertThat(UUID.fromString(firstShipment.getTrackingNumber())).isNotNull();
            assertThat(firstShipment.getTrackingNumber()).isNotEqualTo(secondShipment.getTrackingNumber());
            var history = events.findAllByShipmentIdOrderByOccurredAtAscIdAsc(id(first));
            assertThat(history).hasSize(1);
            assertThat(history.getFirst().getPreviousStatus()).isNull();
            assertThat(history.getFirst().getEmployee()).isNull();
            assertThat(history.getFirst().getNewStatus()).isEqualTo(ShipmentStatus.CREATED);
        });
    }

    @Test
    void callerCannotChooseTrackingNumberAndInvalidRequestsDoNotPersist() throws Exception {
        String valid = body(customer());
        long countBefore = shipments.count();
        mockMvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("{", "{\"trackingNumber\":\"chosen\",")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("2030-01-01T12:00:00Z", "2000-01-01T12:00:00Z")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("60.00", "60.001"))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Long.MAX_VALUE))).andExpect(status().isNotFound());
        assertThat(shipments.count()).isEqualTo(countBefore);
    }

    @Test
    void legacyAssignmentEndpointUsesVersionAndCapacityGuards() throws Exception {
        long customerId = customer();
        String first = create(customerId);
        String second = create(customerId);
        long tripId = new TransactionTemplate(transactionManager).execute(status -> {
            Vehicle vehicle = vehicles.saveAndFlush(new Vehicle(UUID.randomUUID().toString().substring(0, 20), new BigDecimal("100.00")));
            Trip trip = new Trip("Sofia", "Vienna", Instant.parse("2027-01-01T08:00:00Z"), Instant.parse("2027-01-02T08:00:00Z"));
            trip.setVehicle(vehicle);
            return trips.saveAndFlush(trip).getId();
        });
        String endpoint = first + "/trip/" + tripId;
        mockMvc.perform(put(endpoint)).andExpect(status().isBadRequest());
        mockMvc.perform(put(endpoint).param("expectedVersion", "0")).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mockMvc.perform(put(endpoint).param("expectedVersion", "0")).andExpect(status().isConflict());
        mockMvc.perform(put(second + "/trip/" + tripId).param("expectedVersion", "0"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/shipments/trip/" + tripId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/trips/" + tripId + "/shipments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void historyInsertFailureRollsBackNewShipment() {
        long customerId = customer();
        long shipmentCount = shipments.count();
        long eventCount = events.count();
        Long existingMax = jdbc.queryForObject("select coalesce(max(id), 0) from " + SCHEMA + ".tracking_events", Long.class);
        jdbc.execute("ALTER TABLE " + SCHEMA + ".tracking_events ADD CONSTRAINT reject_new_created CHECK (new_status <> 'CREATED' OR id <= " + existingMax + ")");
        try {
            ShipmentRequest request = new ShipmentRequest(customerId, null, "Maria", "Vienna 1",
                    new BigDecimal("10.00"), Instant.parse("2030-01-01T12:00:00Z"));
            assertThatThrownBy(() -> service.create(request)).isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbc.execute("ALTER TABLE " + SCHEMA + ".tracking_events DROP CONSTRAINT reject_new_created");
        }
        assertThat(shipments.count()).isEqualTo(shipmentCount);
        assertThat(events.count()).isEqualTo(eventCount);
    }

    @Test
    void mergedMetadataAndBidirectionalCrewPersistWithoutChangingCoreModel() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 12);
            Vehicle vehicle = new Vehicle("REG-" + suffix, new BigDecimal("100.00"));
            vehicle.setMake("Ford");
            vehicle.setModel("Transit");
            vehicle.setVehicleType("VAN");
            vehicles.saveAndFlush(vehicle);
            Employee employee = new Employee("Ivan", "Petrov", "EMP-" + suffix);
            employee.setEmail(suffix + "@example.com");
            employee.setPhone("+359888123456");
            employees.saveAndFlush(employee);
            Trip trip = new Trip("Sofia", "Vienna", Instant.parse("2027-01-01T08:00:00Z"), Instant.parse("2027-01-02T08:00:00Z"));
            trip.setVehicle(vehicle);
            employee.addTrip(trip);
            assertThat(trip.getEmployees()).containsExactly(employee);
            trips.saveAndFlush(trip);
            Location location = new Location("Bulgaria", "Sofia", "Depot 1");
            location.setLatitude(new BigDecimal("42.6977000"));
            location.setLongitude(new BigDecimal("23.3219000"));
            location.setRecordedAt(Instant.parse("2026-10-26T08:00:00Z"));
            location.setVehicle(vehicle);
            entityManager.persist(location);
            entityManager.flush();
            Long locationId = location.getId();
            entityManager.clear();
            var reloadedEmployee = employees.findById(employee.getId()).orElseThrow();
            assertThat(reloadedEmployee.getEmail()).isEqualTo(suffix + "@example.com");
            assertThat(reloadedEmployee.getTrips()).extracting(Trip::getId).containsExactly(trip.getId());
            var reloadedLocation = entityManager.find(Location.class, locationId);
            assertThat(reloadedLocation.getLatitude()).isEqualByComparingTo("42.6977000");
            assertThat(reloadedLocation.getVehicle().getMake()).isEqualTo("Ford");
            Trip reloadedTrip = trips.findById(trip.getId()).orElseThrow();
            reloadedTrip.removeEmployee(reloadedEmployee);
            assertThat(reloadedEmployee.getTrips()).isEmpty();
            trips.flush();
        });
    }
}