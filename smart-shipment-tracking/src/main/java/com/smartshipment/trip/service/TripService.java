package com.smartshipment.trip.service;

import java.util.List;

import com.smartshipment.employee.entity.Employee;
import com.smartshipment.employee.repository.EmployeeRepository;
import com.smartshipment.shipment.repository.ShipmentRepository;
import com.smartshipment.shipment.entity.ShipmentStatus;
import com.smartshipment.trip.dto.TripRequest;
import com.smartshipment.trip.dto.TripResponse;
import com.smartshipment.trip.entity.Trip;
import com.smartshipment.trip.mapper.TripMapper;
import com.smartshipment.trip.repository.TripRepository;
import com.smartshipment.vehicle.entity.Vehicle;
import com.smartshipment.vehicle.repository.VehicleRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(isolation = Isolation.READ_COMMITTED)
public class TripService {
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final TripMapper tripMapper;
    private final ShipmentRepository shipments;

    public TripService(TripRepository tripRepository, VehicleRepository vehicleRepository,
            EmployeeRepository employeeRepository, TripMapper tripMapper, ShipmentRepository shipments) {
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.employeeRepository = employeeRepository;
        this.tripMapper = tripMapper;
        this.shipments = shipments;
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findAll() {
        return tripRepository.findAllByDeletedFalseOrderByIdAsc().stream()
                .map(tripMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TripResponse findById(@Positive Long id) {
        return tripMapper.toResponse(tripRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> notFound("Trip")));
    }

    public TripResponse create(@Valid TripRequest request) {
        Vehicle vehicle = getActiveVehicle(request.vehicleId());
        Trip trip = tripMapper.toEntity(request);
        trip.setVehicle(vehicle);
        return tripMapper.toResponse(tripRepository.saveAndFlush(trip));
    }

    public TripResponse update(@Positive Long id, @Valid TripRequest request) {
        Trip trip = getForUpdate(id);
        Vehicle vehicle = getActiveVehicle(request.vehicleId());
        if (shipments.totalAssignedWeight(id).compareTo(vehicle.getCapacityKg()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle cannot carry assigned shipment weight");
        }
        tripMapper.updateEntity(trip, request);
        trip.setVehicle(vehicle);
        tripRepository.flush();
        return tripMapper.toResponse(trip);
    }

    public void delete(@Positive Long id) {
        Trip trip = getForUpdate(id);
        if (shipments.existsByTripIdAndStatusNot(id, ShipmentStatus.DELIVERED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trip has undelivered shipments");
        }
        trip.setDeleted(true);
    }

    public TripResponse addEmployee(@Positive Long id, @Positive Long employeeId) {
        Trip trip = getForUpdate(id);
        Employee employee = employeeRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> notFound("Active employee"));
        trip.addEmployee(employee);
        tripRepository.flush();
        return tripMapper.toResponse(trip);
    }

    public void removeEmployee(@Positive Long id, @Positive Long employeeId) {
        Trip trip = getForUpdate(id);
        // Inactive employees can still be removed from an existing crew.
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> notFound("Employee"));
        trip.removeEmployee(employee);
    }

    private Trip getForUpdate(Long id) {
        return tripRepository.findActiveForUpdate(id).orElseThrow(() -> notFound("Trip"));
    }

    private Vehicle getActiveVehicle(Long id) {
        return vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> notFound("Active vehicle"));
    }

    private ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }
}
