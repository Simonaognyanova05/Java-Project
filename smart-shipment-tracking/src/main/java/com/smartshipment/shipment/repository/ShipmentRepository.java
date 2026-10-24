package com.smartshipment.shipment.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.smartshipment.shipment.entity.Shipment;
import com.smartshipment.shipment.entity.ShipmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.id = :id")
    Optional<Shipment> findForUpdate(@Param("id") Long id);

    List<Shipment> findAllByTripIdOrderByIdAsc(Long tripId);

    @Query("select coalesce(sum(s.weightKg), 0) from Shipment s where s.trip.id = :tripId")
    BigDecimal totalAssignedWeight(@Param("tripId") Long tripId);

    boolean existsByTripIdAndStatusNot(Long tripId, ShipmentStatus status);
}
