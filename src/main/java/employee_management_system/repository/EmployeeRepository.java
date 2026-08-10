package employee_management_system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import employee_management_system.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);
}
