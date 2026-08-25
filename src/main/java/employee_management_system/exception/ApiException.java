package employee_management_system.exception;

import employee_management_system.entity.enums.StatusCode;

public class ApiException extends RuntimeException {

    private final StatusCode statusCode;

    public ApiException(StatusCode statusCode) {
        super(statusCode.getDefaultMessage());
        this.statusCode = statusCode;
    }

    public ApiException(StatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }
}
