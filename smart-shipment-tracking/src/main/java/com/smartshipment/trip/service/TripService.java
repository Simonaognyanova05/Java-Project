package com.smartshipment.trip.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartshipment.trip.dto.TripRequest;
import com.smartshipment.trip.dto.TripResponse;
import com.smartshipment.trip.entity.Trip;
import com.smartshipment.trip.mapper.TripMapper;
import com.smartshipment.trip.repository.TripRepository;

@Service
@Transactional
public class TripService {
	private final TripRepository tripRepository;
	private final TripMapper tripMapper;

	public TripService(TripRepository tripRepository, TripMapper tripMapper) {
		this.tripRepository = tripRepository;
		this.tripMapper = tripMapper;
	}

	@Transactional(readOnly = true)
	public List<TripResponse> findAll() {
		return tripRepository.findAll().stream().map(tripMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public TripResponse findById(Long id) {
		return tripMapper.toResponse(getTrip(id));
	}

	public TripResponse create(TripRequest request) {
		if (tripRepository.existsByTrackingNumber(request.trackingNumber())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A trip with this tracking number already exists");
		}
		return tripMapper.toResponse(tripRepository.save(tripMapper.toEntity(request)));
	}

	public TripResponse update(Long id, TripRequest request) {
		Trip trip = getTrip(id);
		if (tripRepository.existsByTrackingNumberAndIdNot(request.trackingNumber(), id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A trip with this tracking number already exists");
		}
		tripMapper.updateEntity(trip, request);
		return tripMapper.toResponse(tripRepository.save(trip));
	}

	public void delete(Long id) {
		if (!tripRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found");
		}
		tripRepository.deleteById(id);
	}

	private Trip getTrip(Long id) {
		return tripRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
	}
}
