package employee_management_system.entity.enums;

// Closed set of legal project statuses; stored in DB as the constant name (ACTIVE / COMPLETED).
public enum ProjectStatus {
    // Project is currently in progress.
    ACTIVE,
    // Project has finished and is no longer active work.
    COMPLETED
}
