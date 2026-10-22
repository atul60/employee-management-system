package employee_management_system.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import employee_management_system.entity.ProjectAssignment;

public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    // Paged query returns Page directly; an empty page means "no assignments", not "no result object".
    @Query ("SELECT pa FROM ProjectAssignment pa WHERE pa.project.id = :projectId")
    public Page<ProjectAssignment> findByProjectId(@Param ("projectId") Long projectId, Pageable pageable);
}
