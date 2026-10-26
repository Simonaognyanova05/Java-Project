package com.smartshipment.trip.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartshipment.trip.dto.TripRequest;
import com.smartshipment.trip.dto.TripResponse;
import com.smartshipment.trip.service.TripService;

import jakarta.validation.Valid;

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
	public TripResponse findById(@PathVariable Long id) {
		return tripService.findById(id);
	}

	@PostMapping
	public ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest request) {
		TripResponse created = tripService.create(request);
		return ResponseEntity.created(URI.create("/api/trips/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public TripResponse update(@PathVariable Long id, @Valid @RequestBody TripRequest request) {
		return tripService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		tripService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
