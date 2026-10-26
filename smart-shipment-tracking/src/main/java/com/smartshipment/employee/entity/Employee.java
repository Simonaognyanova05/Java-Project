package com.smartshipment.employee.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import com.smartshipment.trip.entity.Trip;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.Email;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String firstName;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String lastName;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, unique = true, length = 30)
    private String employeeNumber;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Email
    @Size(max = 254)
    @Column(unique = true, length = 254)
    private String email;

    @Size(max = 30)
    @Column(length = 30)
    private String phone;

    @ManyToMany(mappedBy = "employees")
    private Set<Trip> trips = new HashSet<>();

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Set<Trip> getTrips() { return Collections.unmodifiableSet(trips); }

    public void addTrip(Trip trip) {
        if (trips.add(Objects.requireNonNull(trip, "Trip is required"))) {
            trip.addEmployee(this);
        }
    }

    public void removeTrip(Trip trip) {
        if (trips.remove(trip)) {
            trip.removeEmployee(this);
        }
    }

    protected Employee() {
    }

    public Employee(String firstName, String lastName, String employeeNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.employeeNumber = employeeNumber;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
