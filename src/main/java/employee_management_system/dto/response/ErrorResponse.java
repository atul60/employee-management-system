package employee_management_system.dto.response;

import java.time.LocalDateTime;

import employee_management_system.entity.enums.StatusCode;
import lombok.*;

@Getter
@Setter
public class ErrorResponse {
    private StatusCode statusCode;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}
