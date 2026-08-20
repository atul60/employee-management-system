package employee_management_system.entity.enums;

public enum StatusCode {
    SUCCESS(200),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    NOT_FOUND(404),
    INTERNAL_SERVER_ERROR(500);

    private int code;

    StatusCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
