package employee_management_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import employee_management_system.dto.request.PageQuery;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.dto.response.PageResponse;
import employee_management_system.dto.response.ProjectResponse;
import employee_management_system.service.ProjectService;

@RestController 
@RequestMapping ("/api")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects/list")
    public PageResponse<ProjectResponse> getAllProjects(PageQuery pageQuery) {
        return projectService.getAllProjects(pageQuery);

    }

    @GetMapping("/:{id}/employees")
    public PageResponse<EmployeeResponse> getEmployeesByProjectId(@PathVariable Long id, PageQuery pageQuery) {
        return projectService.getEmployeesByProjectId(id, pageQuery);
    }
}
