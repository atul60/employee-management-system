package employee_management_system.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import employee_management_system.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("""
            SELECT e FROM Employee e
            WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.department.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.designation.title) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Employee> search(@Param("search") String search, Pageable pageable);
}
