package employee_management_system.service;

import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import employee_management_system.dto.request.PageQuery;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.dto.response.PageResponse;
import employee_management_system.dto.response.ProjectResponse;
import employee_management_system.entity.Employee;
import employee_management_system.entity.Project;
import employee_management_system.mapper.EmployeeMapper;
import employee_management_system.mapper.ProjectMapper;
import employee_management_system.repository.EmployeeRepository;
import employee_management_system.repository.ProjectRepository;

@Service 
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    
    public ProjectService(ProjectRepository projectRepository, ProjectMapper projectMapper, EmployeeRepository employeeRepository, EmployeeMapper employeeMapper) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
    }

    public PageResponse<ProjectResponse> getAllProjects(PageQuery pageable) {
        Page<Project> projects = projectRepository.findAll(pageable.toPageable());
        return PageResponse.from(projects.map(projectMapper::toProjectResponse));
    }

    public PageResponse<EmployeeResponse> getEmployeesByProjectId(Long id, PageQuery pageable) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        Page<Employee> employees = employeeRepository.findByProject(project, pageable.toPageable());
        return PageResponse.from(employees.map(employeeMapper::toEmployeeResponse));
    }
}
