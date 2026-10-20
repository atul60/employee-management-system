package employee_management_system.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportsRequest extends PageQuery {
    private String search;
    private Long managerId;
}
