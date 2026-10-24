package employee_management_system.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table (name = "project_assignments")
@Getter 
@Setter 
public class ProjectAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne 
    @JoinColumn (name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne 
    @JoinColumn (name = "project_id")
    private Project project;

    private String role;

    private int allocationPercent;

    private LocalDate startDate;
}
