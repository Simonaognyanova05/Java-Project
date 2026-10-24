package com.smartshipment.trip.controller;

import java.net.URI;
import java.util.List;

import com.smartshipment.trip.dto.TripRequest;
import com.smartshipment.trip.dto.TripResponse;
import com.smartshipment.trip.service.TripService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public List<TripResponse> findAll() {
        return tripService.findAll();
    }

    @GetMapping("/{id}")
    public TripResponse findById(@PathVariable @Positive Long id) {
        return tripService.findById(id);
    }

    @PostMapping
    public ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest request) {
        TripResponse created = tripService.create(request);
        return ResponseEntity.created(URI.create("/api/trips/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TripResponse update(@PathVariable @Positive Long id, @Valid @RequestBody TripRequest request) {
        return tripService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        tripService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/employees/{employeeId}")
    public TripResponse addEmployee(@PathVariable @Positive Long id,
            @PathVariable @Positive Long employeeId) {
        return tripService.addEmployee(id, employeeId);
    }

    @DeleteMapping("/{id}/employees/{employeeId}")
    public ResponseEntity<Void> removeEmployee(@PathVariable @Positive Long id,
            @PathVariable @Positive Long employeeId) {
        tripService.removeEmployee(id, employeeId);
        return ResponseEntity.noContent().build();
    }
}
