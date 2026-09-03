package employee_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import employee_management_system.entity.Designation;

public interface DesignationRepository extends JpaRepository<Designation, Long> {}
