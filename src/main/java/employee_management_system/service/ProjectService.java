package employee_management_system.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import employee_management_system.dto.request.Page.PageQuery;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.dto.response.PageResponse;
import employee_management_system.dto.response.ProjectResponse;
import employee_management_system.entity.Employee;
import employee_management_system.entity.Project;
import employee_management_system.entity.ProjectAssignment;
import employee_management_system.entity.enums.StatusCode;
import employee_management_system.exception.ApiException;
import employee_management_system.mapper.EmployeeMapper;
import employee_management_system.mapper.ProjectMapper;
import employee_management_system.repository.ProjectAssignmentRepository;
import employee_management_system.repository.ProjectRepository;
import employee_management_system.repository.EmployeeRepository;

@Service 
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final EmployeeMapper employeeMapper;
    private final ProjectAssignmentRepository projectAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    
    public ProjectService(
        ProjectRepository projectRepository, 
        ProjectMapper projectMapper, 
        EmployeeMapper employeeMapper,
        ProjectAssignmentRepository projectAssignmentRepository,
        EmployeeRepository employeeRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
        this.employeeMapper = employeeMapper;
        this.projectAssignmentRepository = projectAssignmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public PageResponse<ProjectResponse> getAllProjects(PageQuery pageable) {
        Page<Project> projects = projectRepository.findAll(pageable.toPageable());
        return PageResponse.from(projects.map(projectMapper::toProjectResponse));
    }

    public PageResponse<EmployeeResponse> getEmployeesByProjectId(Long id, PageQuery pageable) {
        // Existence is checked on projects: an empty assignment page cannot distinguish missing project from zero assignees.
        if (!projectRepository.existsById(id)) {
            throw new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Project not found with id: " + id);
        }
        Page<ProjectAssignment> assignments = projectAssignmentRepository.findByProjectId(id, pageable.toPageable());
        // Each assignment wraps one employee; unwrap before mapping to the employee API shape.
        Page<Employee> employees = assignments.map(assignment -> assignment.getEmployee());
        return PageResponse.from(employees.map(employeeMapper::toResponse));
    }

    public void addEmployeeToProject(Long projectId, Long employeeId) {
        if(!projectRepository.existsById(projectId)) {
            throw new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Project not found with id: " + projectId);
        }
        if(!employeeRepository.existsById(employeeId)) {
            throw new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Employee not found with id: " + employeeId);
        }
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Project not found with id: " + projectId));
        ProjectAssignment newProjectAssignment = new ProjectAssignment();
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Employee not found with id: " + employeeId));
        newProjectAssignment.setEmployee(employee);
        newProjectAssignment.setProject(project);
        newProjectAssignment.setRole("LEAD");
        newProjectAssignment.setAllocationPercent(100);
        newProjectAssignment.setStartDate(LocalDate.now());
        projectAssignmentRepository.save(newProjectAssignment);
    }
}
