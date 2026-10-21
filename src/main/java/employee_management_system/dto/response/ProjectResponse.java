package employee_management_system.dto.response;

import employee_management_system.entity.enums.ProjectStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectResponse {
    private Long id;
    private String name;
    private String code;
    // Same enum as the entity so JSON can only ever carry ACTIVE or COMPLETED.
    private ProjectStatus status;
}
