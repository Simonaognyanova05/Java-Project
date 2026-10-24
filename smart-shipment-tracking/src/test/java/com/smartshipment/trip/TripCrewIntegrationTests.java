package com.smartshipment.trip;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.smartshipment.employee.entity.Employee;
import com.smartshipment.trip.entity.Trip;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TripCrewIntegrationTests {
    @Autowired
    private EntityManager entityManager;

    @Test
    void employeesCanServeMultipleTripsAndRemovingMembershipPreservesEmployees() {
        Employee first = new Employee("Ivan", "Petrov", "EMP-001");
        Employee second = new Employee("Maria", "Ivanova", "EMP-002");
        entityManager.persist(first);
        entityManager.persist(second);
        Instant departure = Instant.parse("2027-01-01T08:00:00Z");
        Instant arrival = departure.plusSeconds(3600);
        Trip firstTrip = new Trip("Sofia", "Plovdiv", departure, arrival);
        Trip secondTrip = new Trip("Plovdiv", "Sofia", departure, arrival);
        firstTrip.addEmployee(first);
        firstTrip.addEmployee(first);
        firstTrip.addEmployee(second);
        secondTrip.addEmployee(first);
        entityManager.persist(firstTrip);
        entityManager.persist(secondTrip);
        entityManager.flush();
        entityManager.clear();

        Trip reloaded = entityManager.find(Trip.class, firstTrip.getId());
        assertThat(reloaded.getEmployees()).extracting(Employee::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
        Trip other = entityManager.find(Trip.class, secondTrip.getId());
        assertThat(other.getEmployees()).extracting(Employee::getId).containsExactly(first.getId());

        Employee crewMember = entityManager.find(Employee.class, first.getId());
        reloaded.removeEmployee(crewMember);
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(Trip.class, firstTrip.getId()).getEmployees())
                .extracting(Employee::getId).containsExactly(second.getId());
        assertThat(entityManager.find(Employee.class, first.getId())).isNotNull();
        assertThat(entityManager.find(Trip.class, secondTrip.getId()).getEmployees())
                .extracting(Employee::getId).containsExactly(first.getId());
    }
}
