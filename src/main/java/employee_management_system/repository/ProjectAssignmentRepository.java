package employee_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import employee_management_system.entity.ProjectAssignment;

public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    public List<ProjectAssignment> findByEmployeeId(Long employeeId);

    // One bridge row for this project+employee pair; empty when they are not linked.
    Optional<ProjectAssignment> findByProjectIdAndEmployeeId(Long projectId, Long employeeId);

    // Paged query returns Page directly; an empty page means "no assignments", not "no result object".
    @Query ("SELECT pa FROM ProjectAssignment pa WHERE pa.project.id = :projectId")
    public Page<ProjectAssignment> findByProjectId(@Param ("projectId") Long projectId, Pageable pageable);

    // Query the bridge entity; optional search matches project name or code.
    @Query("""
            SELECT pa FROM ProjectAssignment pa
            WHERE pa.employee.id = :employeeId
              AND (
                    :search IS NULL
                    OR LOWER(pa.project.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(pa.project.code) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            """)
    Page<ProjectAssignment> findAllFilteredByEmployeeId(
            @Param("search") String search,
            @Param("employeeId") Long employeeId,
            Pageable pageable);
}
