package com.smartshipment.customer.mapper;

import org.springframework.stereotype.Component;

import com.smartshipment.customer.dto.CustomerRequest;
import com.smartshipment.customer.dto.CustomerResponse;
import com.smartshipment.customer.entity.Customer;

@Component
public class CustomerMapper {
	public Customer toEntity(CustomerRequest request) {
		return new Customer(request.firstName(), request.lastName(), request.email(), request.phone());
	}

	public void updateEntity(Customer customer, CustomerRequest request) {
		customer.setFirstName(request.firstName());
		customer.setLastName(request.lastName());
		customer.setEmail(request.email());
		customer.setPhone(request.phone());
	}

	public CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getFirstName(), customer.getLastName(),
				customer.getEmail(), customer.getPhone());
	}
}