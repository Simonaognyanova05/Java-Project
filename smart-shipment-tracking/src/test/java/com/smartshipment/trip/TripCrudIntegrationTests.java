package com.smartshipment.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.UUID;

import com.smartshipment.employee.entity.Employee;
import com.smartshipment.employee.repository.EmployeeRepository;
import com.smartshipment.trip.repository.TripRepository;
import com.smartshipment.vehicle.entity.Vehicle;
import com.smartshipment.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TripCrudIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private TripRepository trips;
    @Autowired private VehicleRepository vehicles;
    @Autowired private EmployeeRepository employees;

    private Vehicle vehicle() {
        return vehicles.saveAndFlush(new Vehicle(UUID.randomUUID().toString().substring(0, 20),
                new BigDecimal("1000.00")));
    }

    private String request(Long vehicleId, String origin) {
        return request(vehicleId, origin, null);
    }

    private String request(Long vehicleId, String origin, Long expectedVersion) {
        String versionField = expectedVersion == null ? "" : ", \"expectedVersion\": " + expectedVersion;
        return """
                {"origin":"%s","destination":"Vienna","departureAt":"2027-01-01T08:00:00Z",
                 "expectedArrivalAt":"2027-01-02T08:00:00Z","vehicleId":%d%s}
                """.formatted(origin, vehicleId, versionField);
    }

    @Test
    void crudAndCrewManagementPreserveHistoricalRecords() throws Exception {
        Vehicle vehicle = vehicle();
        Employee employee = employees.saveAndFlush(new Employee("Ivan", "Petrov", "TRIP-EMP-1"));
        String location = mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(request(vehicle.getId(), "Sofia")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleId").value(vehicle.getId()))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        Long id = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
        mockMvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("Sofia"));
        mockMvc.perform(get("/api/trips")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isNotEmpty());
        mockMvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
                        .content(request(vehicle.getId(), "Plovdiv", 0L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.origin").value("Plovdiv"))
                .andExpect(jsonPath("$.version").value(1));
        String crewPath = location + "/employees/" + employee.getId();
        mockMvc.perform(put(crewPath)).andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeIds.length()").value(1));
        mockMvc.perform(put(crewPath)).andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeIds.length()").value(1));
        employee.setDeleted(true);
        employees.flush();
        mockMvc.perform(delete(crewPath)).andExpect(status().isNoContent());
        mockMvc.perform(get(location)).andExpect(jsonPath("$.employeeIds").isEmpty());
        mockMvc.perform(delete(location)).andExpect(status().isNoContent());
        mockMvc.perform(get(location)).andExpect(status().isNotFound());
        mockMvc.perform(put(crewPath)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/trips"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
        assertThat(trips.findById(id).orElseThrow().isDeleted()).isTrue();
        assertThat(employees.existsById(employee.getId())).isTrue();
        assertThat(vehicles.existsById(vehicle.getId())).isTrue();
    }

    @Test
    void invalidScheduleAndMissingVehicleAreRejected() throws Exception {
        Vehicle vehicle = vehicle();
        String body = request(vehicle.getId(), "Sofia");
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("2027-01-02T08:00:00Z", "2027-01-01T08:00:00Z")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"vehicleId\":" + vehicle.getId(), "\"vehicleId\":null")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/trips/0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/trips/9223372036854775807")).andExpect(status().isNotFound());
        assertThat(trips.findAllByDeletedFalseOrderByIdAsc()).isEmpty();
    }

    @Test
    void inactiveVehicleAndEmployeeCannotBeAssigned() throws Exception {
        Vehicle vehicle = vehicle();
        vehicle.setDeleted(true);
        vehicles.flush();
        mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(request(vehicle.getId(), "Sofia")))
                .andExpect(status().isNotFound());
        vehicle.setDeleted(false);
        vehicles.flush();
        String location = mockMvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(request(vehicle.getId(), "Sofia")))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        Employee employee = new Employee("Maria", "Petrova", "TRIP-EMP-2");
        employee.setDeleted(true);
        employees.saveAndFlush(employee);
        mockMvc.perform(put(location + "/employees/" + employee.getId())).andExpect(status().isNotFound());
        mockMvc.perform(get(location)).andExpect(jsonPath("$.employeeIds").isEmpty());
    }
}
