package com.smartshipment.employee.repository;

import java.util.Optional;
import com.smartshipment.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByIdAndDeletedFalse(Long id);
}
