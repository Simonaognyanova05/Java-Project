package com.smartshipment.shipment;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.smartshipment.customer.entity.Customer;
import com.smartshipment.customer.repository.CustomerRepository;
import com.smartshipment.shipment.dto.AssignTripRequest;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.shipment.service.ShipmentAssignmentService;
import com.smartshipment.trip.dto.TripRequest;
import com.smartshipment.trip.entity.Trip;
import com.smartshipment.trip.repository.TripRepository;
import com.smartshipment.trip.service.TripService;
import com.smartshipment.vehicle.entity.Vehicle;
import com.smartshipment.vehicle.repository.VehicleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShipmentAssignmentIntegrationTests {
    @Autowired private ShipmentAssignmentService assignments;
    @Autowired private ShipmentRepository shipments;
    @Autowired private TripRepository trips;
    @Autowired private TripService tripService;
    @Autowired private CustomerRepository customers;
    @Autowired private VehicleRepository vehicles;
    @Autowired private EntityManager entityManager;
    @Autowired private MockMvc mockMvc;

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
    void assignmentReassignmentAndIdempotentRequestReserveWeightOnce() throws Exception {
        Trip first = trip("100");
        Trip second = trip("100");
        Shipment shipment = shipment("60");
        String endpoint = "/api/shipments/" + shipment.getId() + "/trip";
        mockMvc.perform(put(endpoint).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":" + first.getId() + ",\"expectedVersion\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tripId").value(first.getId()))
                .andExpect(jsonPath("$.version").value(1));
        assignments.assign(shipment.getId(), new AssignTripRequest(first.getId(), 1L));
        assertThat(shipments.totalAssignedWeight(first.getId())).isEqualByComparingTo("60");
        assignments.assign(shipment.getId(), new AssignTripRequest(second.getId(), 1L));
        entityManager.clear();
        assertThat(shipments.totalAssignedWeight(first.getId())).isEqualByComparingTo("0");
        assertThat(shipments.totalAssignedWeight(second.getId())).isEqualByComparingTo("60");
        mockMvc.perform(get("/api/trips/" + second.getId() + "/shipments"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(shipment.getId()));
    }

    @Test
    void overCapacityAndStaleVersionReturnConflictWithoutChangingAssignment() throws Exception {
        Trip oldTrip = trip("100");
        Trip fullTrip = trip("100");
        Shipment existing = shipment("70");
        assignments.assign(existing.getId(), new AssignTripRequest(fullTrip.getId(), 0L));
        Shipment moving = shipment("40");
        assignments.assign(moving.getId(), new AssignTripRequest(oldTrip.getId(), 0L));
        String endpoint = "/api/shipments/" + moving.getId() + "/trip";
        mockMvc.perform(put(endpoint).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":" + fullTrip.getId() + ",\"expectedVersion\":1}"))
                .andExpect(status().isConflict());
        entityManager.clear();
        assertThat(shipments.findById(moving.getId()).orElseThrow().getTrip().getId()).isEqualTo(oldTrip.getId());
        assertThat(shipments.totalAssignedWeight(fullTrip.getId())).isEqualByComparingTo("70");
        mockMvc.perform(put(endpoint).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":" + oldTrip.getId() + ",\"expectedVersion\":0}"))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = {"LOADED", "IN_TRANSIT", "DELIVERED"})
    void shipmentCannotChangeTripAfterLoading(ShipmentStatus shipmentStatus) throws Exception {
        Trip target = trip("100");
        Shipment shipment = shipment("20");
        // Fixture setup only; production status changes belong to the lifecycle service.
        entityManager.createNativeQuery("update shipments set status = :status where id = :id")
                .setParameter("status", shipmentStatus.name()).setParameter("id", shipment.getId()).executeUpdate();
        entityManager.clear();
        mockMvc.perform(put("/api/shipments/" + shipment.getId() + "/trip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":" + target.getId() + ",\"expectedVersion\":0}"))
                .andExpect(status().isConflict());
        assertThat(shipments.findById(shipment.getId()).orElseThrow().getTrip()).isNull();
    }

    @Test
    void inactiveTripVehicleMissingResourcesAndInvalidInputAreRejected() throws Exception {
        Trip target = trip("100");
        Shipment shipment = shipment("20");
        target.setDeleted(true);
        trips.flush();
        String endpoint = "/api/shipments/" + shipment.getId() + "/trip";
        String body = "{\"tripId\":" + target.getId() + ",\"expectedVersion\":0}";
        mockMvc.perform(put(endpoint).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        target.setDeleted(false);
        target.getVehicle().setDeleted(true);
        vehicles.flush();
        mockMvc.perform(put(endpoint).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(put(endpoint).contentType(MediaType.APPLICATION_JSON).content("{\"tripId\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/shipments/9223372036854775807/trip")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(endpoint).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":9223372036854775807,\"expectedVersion\":0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignedShipmentsPreventDeactivationAndUndersizedVehicleChange() {
        Trip target = trip("100");
        Trip small = trip("10");
        Shipment shipment = shipment("20");
        assignments.assign(shipment.getId(), new AssignTripRequest(target.getId(), 0L));
        assertThatThrownBy(() -> tripService.delete(target.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> tripService.update(target.getId(), new TripRequest("Sofia", "Vienna",
                target.getDepartureAt(), target.getExpectedArrivalAt(), small.getVehicle().getId())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(target.isDeleted()).isFalse();
    }
}