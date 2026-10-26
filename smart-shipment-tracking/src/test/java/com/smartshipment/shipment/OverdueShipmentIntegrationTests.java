package com.smartshipment.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.smartshipment.customer.entity.Customer;
import com.smartshipment.customer.repository.CustomerRepository;
import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.shipment.service.OverdueShipmentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(OverdueShipmentIntegrationTests.FixedClockConfiguration.class)
@Transactional
class OverdueShipmentIntegrationTests {
    private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");
    @Autowired private ShipmentRepository shipments;
    @Autowired private CustomerRepository customers;
    @Autowired private EntityManager entityManager;
    @Autowired private OverdueShipmentService service;
    @Autowired private MockMvc mockMvc;

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    private Shipment shipment(Instant deadline, ShipmentStatus status) {
        Customer customer = customers.saveAndFlush(new Customer("Ivan", "Petrov",
                UUID.randomUUID() + "@example.com", null));
        Shipment shipment = new Shipment(customer, new BigDecimal("10.00"));
        shipment.setExpectedDeliveryAt(deadline);
        shipments.saveAndFlush(shipment);
        // Fixture setup; this does not represent the production lifecycle operation.
        entityManager.createQuery("update Shipment s set s.status = :status where s.id = :id")
                .setParameter("status", status).setParameter("id", shipment.getId()).executeUpdate();
        entityManager.clear();
        return shipments.findById(shipment.getId()).orElseThrow();
    }

    @Test
    void queryIncludesAllOverdueUndeliveredStatusesAndExcludesOtherShipments() {
        for (ShipmentStatus status : ShipmentStatus.values()) {
            shipment(NOW.minusSeconds(60), status);
        }
        shipment(NOW, ShipmentStatus.CREATED);
        shipment(NOW.plusSeconds(60), ShipmentStatus.ACCEPTED);
        shipment(null, ShipmentStatus.IN_TRANSIT);
        List<Shipment> result = shipments.findOverdueShipments(NOW, ShipmentStatus.DELIVERED,
                PageRequest.of(0, 20));
        assertThat(result).extracting(Shipment::getStatus).containsExactly(
                ShipmentStatus.CREATED, ShipmentStatus.ACCEPTED,
                ShipmentStatus.LOADED, ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void endpointUsesInjectedClockAndStableDeadlineAndIdOrderingWithPagination() throws Exception {
        Shipment later = shipment(NOW.minusSeconds(60), ShipmentStatus.CREATED);
        Shipment first = shipment(NOW.minusSeconds(120), ShipmentStatus.ACCEPTED);
        Shipment second = shipment(NOW.minusSeconds(120), ShipmentStatus.IN_TRANSIT);
        mockMvc.perform(get("/api/shipments/overdue").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(first.getId()))
                .andExpect(jsonPath("$[0].expectedDeliveryAt").value("2026-10-24T11:58:00Z"))
                .andExpect(jsonPath("$[0].customer").doesNotExist());
        mockMvc.perform(get("/api/shipments/overdue").param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(second.getId()));
        assertThat(service.findOverdue(2, 1)).extracting(response -> response.id()).containsExactly(later.getId());
        assertThat(service.findOverdue(3, 1)).isEmpty();
    }

    @Test
    void invalidPaginationIsRejectedAndEmptyResultsReturnAnEmptyArray() throws Exception {
        mockMvc.perform(get("/api/shipments/overdue")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(get("/api/shipments/overdue").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/shipments/overdue").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/shipments/overdue").param("size", "101"))
                .andExpect(status().isBadRequest());
    }
}