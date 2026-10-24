package com.smartshipment.trip.repository;

import java.util.List;
import java.util.Optional;

import com.smartshipment.trip.entity.Trip;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long> {
    @EntityGraph(attributePaths = {"vehicle", "employees"})
    List<Trip> findAllByDeletedFalseOrderByIdAsc();

    @EntityGraph(attributePaths = {"vehicle", "employees"})
    Optional<Trip> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id and t.deleted = false")
    Optional<Trip> findActiveForUpdate(@Param("id") Long id);
}
