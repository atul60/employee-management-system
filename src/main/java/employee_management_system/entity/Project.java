package employee_management_system.entity;

import employee_management_system.entity.enums.ProjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table (name = "projects")
@Getter 
@Setter 
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull 
    private String name;
    @NotNull 
    @Column (name = "code", unique = true)
    private String code;
    // Enum type restricts status to ACTIVE/COMPLETED; STRING stores the constant name in the varchar column.
    @Enumerated(EnumType.STRING)
    @NotNull
    private ProjectStatus status = ProjectStatus.ACTIVE;
}
