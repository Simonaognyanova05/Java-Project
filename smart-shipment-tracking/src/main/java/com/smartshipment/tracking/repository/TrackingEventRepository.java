package com.smartshipment.tracking.repository;

import java.util.List;
import com.smartshipment.tracking.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findAllByShipmentIdOrderByOccurredAtAscIdAsc(Long shipmentId);
}
