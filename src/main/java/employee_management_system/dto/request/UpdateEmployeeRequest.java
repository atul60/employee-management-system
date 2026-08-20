package employee_management_system.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import employee_management_system.entity.enums.EmployeeStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmployeeRequest {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String department;
    private String designation;
    private BigDecimal salary;
    private LocalDate joiningDate;
    private EmployeeStatus status;
}