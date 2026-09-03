package employee_management_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import employee_management_system.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {}
