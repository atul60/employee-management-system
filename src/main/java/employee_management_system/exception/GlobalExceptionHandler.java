package employee_management_system.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import employee_management_system.dto.response.ErrorResponse;
import employee_management_system.entity.enums.StatusCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        StatusCode statusCode = ex.getStatusCode();
        ErrorResponse body = ErrorResponse.of(statusCode, ex.getMessage());
        return ResponseEntity.status(statusCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        StatusCode statusCode = StatusCode.VALIDATION_FAILED;
        ErrorResponse body = ErrorResponse.of(statusCode, statusCode.getDefaultMessage(), fieldErrors);
        return ResponseEntity.status(statusCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(HttpMessageNotReadableException ex) {
        StatusCode statusCode = StatusCode.MALFORMED_REQUEST;
        ErrorResponse body = ErrorResponse.of(statusCode);
        return ResponseEntity.status(statusCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        StatusCode statusCode = StatusCode.INTERNAL_SERVER_ERROR;
        ErrorResponse body = ErrorResponse.of(statusCode);
        return ResponseEntity.status(statusCode.getHttpStatus()).body(body);
    }
}
