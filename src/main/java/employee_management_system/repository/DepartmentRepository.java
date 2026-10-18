package employee_management_system.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import employee_management_system.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Query ("""
            SELECT d FROM Department d
            WHERE (:search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY d.name ASC
            """)
    Page<Department> findAllFiltered(@Param ("search") String search, Pageable pageable);
}
