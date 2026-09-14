package employee_management_system.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageQuery {
    private int pageNumber = 1;
    private int pageSize = 10;
    private String sortBy = "id";
    private String sortDirection = "asc";

    public Pageable toPageable() {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        String property = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        return PageRequest.of(pageNumber - 1, pageSize, Sort.by(direction, property));
    }
}
