package employee_management_system.exception;

import employee_management_system.entity.enums.StatusCode;

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(StatusCode statusCode) {
        super(statusCode);
    }

    public DuplicateResourceException(StatusCode statusCode, String message) {
        super(statusCode, message);
    }
}
