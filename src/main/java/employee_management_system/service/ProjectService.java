package employee_management_system.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** Deletes the assignment row only; the employee and the project stay. */
    public void removeEmployeeFromProject(Long projectId, Long employeeId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Project not found with id: " + projectId);
        }
        if (!employeeRepository.existsById(employeeId)) {
            throw new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Employee not found with id: " + employeeId);
        }
        ProjectAssignment assignment = projectAssignmentRepository
                .findByProjectIdAndEmployeeId(projectId, employeeId)
                .orElseThrow(() -> new ApiException(
                        StatusCode.RESOURCE_NOT_FOUND, "Employee is not assigned to this project"));
        projectAssignmentRepository.delete(assignment);
    }

    /**
     * Moves an employee from one project to another (delete old bridge, insert new).
     * Target project is resolved AFTER the delete on purpose so a mid-method failure
     * shows why a shared transaction is required for atomicity.
     * @Transactional keeps both writes in one unit: any runtime failure rolls back the delete.
     */
    @Transactional
    public void transferEmployeeToProject(Long fromProjectId, Long employeeId, Long toProjectId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Employee not found with id: " + employeeId);
        }
        if (fromProjectId.equals(toProjectId)) {
            throw new ApiException(StatusCode.VALIDATION_FAILED, "Source and target project must be different");
        }
        ProjectAssignment oldAssignment = projectAssignmentRepository
                .findByProjectIdAndEmployeeId(fromProjectId, employeeId)
                .orElseThrow(() -> new ApiException(
                        StatusCode.RESOURCE_NOT_FOUND, "Employee is not assigned to this project"));
        if (projectAssignmentRepository.findByProjectIdAndEmployeeId(toProjectId, employeeId).isPresent()) {
            throw new ApiException(
                    StatusCode.VALIDATION_FAILED, "Employee is already assigned to the target project");
        }

        // Capture fields before delete; the entity may not be usable after removal.
        String role = oldAssignment.getRole();
        int allocationPercent = oldAssignment.getAllocationPercent();
        Employee employee = oldAssignment.getEmployee();

        // First write: without a surrounding transaction this can commit on its own.
        projectAssignmentRepository.delete(oldAssignment);

        // Second step can fail (missing target) after the delete above has already run.
        Project toProject = projectRepository.findById(toProjectId)
                .orElseThrow(() -> new ApiException(
                        StatusCode.RESOURCE_NOT_FOUND, "Project not found with id: " + toProjectId));

        ProjectAssignment newAssignment = new ProjectAssignment();
        newAssignment.setEmployee(employee);
        newAssignment.setProject(toProject);
        newAssignment.setRole(role);
        newAssignment.setAllocationPercent(allocationPercent);
        newAssignment.setStartDate(LocalDate.now());
        projectAssignmentRepository.save(newAssignment);
    }
}
