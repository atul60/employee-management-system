package employee_management_system.mapper;

import org.springframework.stereotype.Component;

import employee_management_system.dto.response.ProjectResponse;
import employee_management_system.entity.Project;

@Component 
public class ProjectMapper {

    public ProjectResponse toProjectResponse(Project project) {
        ProjectResponse projectResponse = new ProjectResponse();
        projectResponse.setId(project.getId());
        projectResponse.setName(project.getName());
        projectResponse.setCode(project.getCode());
        projectResponse.setStatus(project.getStatus());
        return projectResponse;
    }
}
