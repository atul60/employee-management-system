package employee_management_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import employee_management_system.dto.request.Page.PageQuery;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.dto.response.PageResponse;
import employee_management_system.dto.response.ProjectResponse;
import employee_management_system.service.ProjectService;

@RestController
@RequestMapping ("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/list")
    public PageResponse<ProjectResponse> getAllProjects(PageQuery pageQuery) {
        return projectService.getAllProjects(pageQuery);

    }

    // Nested resource: employees assigned to the project named in the path.
    @GetMapping("/{projectId}/employees")
    public PageResponse<EmployeeResponse> getEmployeesByProjectId(@PathVariable Long projectId, PageQuery pageQuery) {
        return projectService.getEmployeesByProjectId(projectId, pageQuery);
    }

    @PostMapping("/{projectId}/employees/{employeeId}")
    public void addEmployeeToProject(@PathVariable Long projectId, @PathVariable Long employeeId) {
        projectService.addEmployeeToProject(projectId, employeeId);
    }
}
