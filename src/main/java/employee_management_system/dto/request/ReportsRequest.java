package employee_management_system.dto.request;

import employee_management_system.dto.request.Page.SearchedPageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportsRequest extends SearchedPageQuery {
    private Long managerId;
}
