package employee_management_system.entity.enums;

public enum EmployeeStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    ON_LEAVE("On Leave"),
    TERMINATED("Terminated");

    private String status;

    EmployeeStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
