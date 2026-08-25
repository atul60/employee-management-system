package employee_management_system.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import employee_management_system.entity.enums.StatusCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private StatusCode statusCode;
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> fieldErrors;

    public static ErrorResponse of(StatusCode statusCode) {
        return of(statusCode, statusCode.getDefaultMessage(), null);
    }

    public static ErrorResponse of(StatusCode statusCode, String message) {
        return of(statusCode, message, null);
    }

    public static ErrorResponse of(StatusCode statusCode, String message, Map<String, String> fieldErrors) {
        ErrorResponse response = new ErrorResponse();
        response.setStatusCode(statusCode);
        response.setStatus(statusCode.getHttpStatus().value());
        response.setError(statusCode.getHttpStatus().getReasonPhrase());
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        response.setFieldErrors(fieldErrors);
        return response;
    }
}
