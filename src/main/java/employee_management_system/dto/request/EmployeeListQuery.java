package employee_management_system.dto.request;

import employee_management_system.entity.enums.EmployeeStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeListQuery extends PageQuery {
    private String search;
    private EmployeeStatus status;
    private Long departmentId;
    private Long designationId;
}
