package com.smartshipment.customer.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartshipment.customer.dto.CustomerRequest;
import com.smartshipment.customer.dto.CustomerResponse;
import com.smartshipment.customer.entity.Customer;
import com.smartshipment.customer.mapper.CustomerMapper;
import com.smartshipment.customer.repository.CustomerRepository;

@Service
@Transactional
public class CustomerService {
	private final CustomerRepository customerRepository;
	private final CustomerMapper customerMapper;

	public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
		this.customerRepository = customerRepository;
		this.customerMapper = customerMapper;
	}

	@Transactional(readOnly = true)
	public List<CustomerResponse> findAll() {
		return customerRepository.findAll().stream().map(customerMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public CustomerResponse findById(Long id) {
		return customerMapper.toResponse(getCustomer(id));
	}

	public CustomerResponse create(CustomerRequest request) {
		if (customerRepository.existsByEmail(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A customer with this email already exists");
		}
		return customerMapper.toResponse(customerRepository.save(customerMapper.toEntity(request)));
	}

	public CustomerResponse update(Long id, CustomerRequest request) {
		Customer customer = getCustomer(id);
		if (customerRepository.existsByEmailAndIdNot(request.email(), id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A customer with this email already exists");
		}
		customerMapper.updateEntity(customer, request);
		return customerMapper.toResponse(customerRepository.save(customer));
	}

	public void delete(Long id) {
		if (!customerRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
		}
		customerRepository.deleteById(id);
	}

	private Customer getCustomer(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
	}
}