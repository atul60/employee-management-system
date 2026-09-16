package employee_management_system.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeListQuery extends PageQuery {
    private String search;
}
