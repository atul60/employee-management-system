package employee_management_system.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEmployeeRequest {
    private String firstName;
    private String lastName;
    @Email
    @NotBlank
    private String email;
    @Pattern(regexp = "^\\d{10}$")
    @NotBlank
    private String phoneNumber;
    private String department;
    private String designation;
    private BigDecimal salary;
    private LocalDate joiningDate;
}
