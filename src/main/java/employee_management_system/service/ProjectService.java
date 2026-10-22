package employee_management_system.service;

import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import employee_management_system.dto.request.PageQuery;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.dto.response.PageResponse;
import employee_management_system.dto.response.ProjectResponse;
import employee_management_system.entity.Employee;
import employee_management_system.entity.Project;
import employee_management_system.entity.ProjectAssignment;
import employee_management_system.entity.enums.StatusCode;
import employee_management_system.exception.ResourceNotFoundException;
import employee_management_system.mapper.EmployeeMapper;
import employee_management_system.mapper.ProjectMapper;
import employee_management_system.repository.ProjectAssignmentRepository;
import employee_management_system.repository.ProjectRepository;

@Service 
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final EmployeeMapper employeeMapper;
    private final ProjectAssignmentRepository projectAssignmentRepository;
    
    public ProjectService(
        ProjectRepository projectRepository, 
        ProjectMapper projectMapper, 
        EmployeeMapper employeeMapper,
        ProjectAssignmentRepository projectAssignmentRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
        this.employeeMapper = employeeMapper;
        this.projectAssignmentRepository = projectAssignmentRepository;
    }

    public PageResponse<ProjectResponse> getAllProjects(PageQuery pageable) {
        Page<Project> projects = projectRepository.findAll(pageable.toPageable());
        return PageResponse.from(projects.map(projectMapper::toProjectResponse));
    }

    public PageResponse<EmployeeResponse> getEmployeesByProjectId(Long id, PageQuery pageable) {
        // Existence is checked on projects: an empty assignment page cannot distinguish missing project from zero assignees.
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException(StatusCode.PROJECT_NOT_FOUND, "Project not found with id: " + id);
        }
        Page<ProjectAssignment> assignments = projectAssignmentRepository.findByProjectId(id, pageable.toPageable());
        // Each assignment wraps one employee; unwrap before mapping to the employee API shape.
        Page<Employee> employees = assignments.map(assignment -> assignment.getEmployee());
        return PageResponse.from(employees.map(employeeMapper::toResponse));
    }
}
