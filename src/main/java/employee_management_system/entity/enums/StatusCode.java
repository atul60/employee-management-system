package employee_management_system.entity.enums;

import org.springframework.http.HttpStatus;

public enum StatusCode {
    DUPLICATE_EMAIL(409, "Employee with this email already exists"),
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "Employee with this phone number already exists"),
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "Employee not found"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request body"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");


    private final HttpStatus httpStatus;
    private final String defaultMessage;

    StatusCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    StatusCode(int httpStatus, String defaultMessage) {
        this.httpStatus = HttpStatus.valueOf(httpStatus);
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
