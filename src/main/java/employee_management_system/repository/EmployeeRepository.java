package employee_management_system.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import employee_management_system.entity.Employee;
import employee_management_system.entity.enums.EmployeeStatus;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("""
            SELECT e FROM Employee e
            WHERE (:status IS NULL OR e.status = :status)
              AND (:departmentId IS NULL OR e.department.id = :departmentId)
              AND (:designationId IS NULL OR e.designation.id = :designationId)
              AND (
                    :search IS NULL
                    OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(e.department.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(e.designation.title) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            """)
    Page<Employee> findAllFiltered(
            @Param("search") String search,
            @Param("status") EmployeeStatus status,
            @Param("departmentId") Long departmentId,
            @Param("designationId") Long designationId,
            Pageable pageable);
}
