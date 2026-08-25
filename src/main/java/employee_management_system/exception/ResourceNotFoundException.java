package employee_management_system.exception;

import employee_management_system.entity.enums.StatusCode;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(StatusCode statusCode) {
        super(statusCode);
    }

    public ResourceNotFoundException(StatusCode statusCode, String message) {
        super(statusCode, message);
    }
}
