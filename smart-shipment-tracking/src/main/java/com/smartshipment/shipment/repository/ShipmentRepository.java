package com.smartshipment.shipment.repository;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
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
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.id = :id")
    Optional<Shipment> findForUpdate(@Param("id") Long id);

    List<Shipment> findAllByTripIdOrderByIdAsc(Long tripId);

    @Query("select coalesce(sum(s.weightKg), 0) from Shipment s where s.trip.id = :tripId")
    BigDecimal totalAssignedWeight(@Param("tripId") Long tripId);

    @Query("""
            SELECT s FROM Shipment s
            WHERE s.expectedDeliveryAt < :now
              AND s.status <> :deliveredStatus
            ORDER BY s.expectedDeliveryAt ASC, s.id ASC
            """)
    List<Shipment> findOverdueShipments(@Param("now") Instant now,
            @Param("deliveredStatus") ShipmentStatus deliveredStatus, Pageable pageable);

    boolean existsByTripIdAndStatusNot(Long tripId, ShipmentStatus status);
}
