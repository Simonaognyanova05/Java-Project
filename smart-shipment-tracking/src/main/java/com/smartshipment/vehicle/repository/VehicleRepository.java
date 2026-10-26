package com.smartshipment.vehicle.repository;

import java.util.Optional;
import com.smartshipment.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByIdAndDeletedFalse(Long id);
}
