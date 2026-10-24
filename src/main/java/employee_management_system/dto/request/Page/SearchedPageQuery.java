package employee_management_system.dto.request.Page;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchedPageQuery extends PageQuery {
    private String search;
}
