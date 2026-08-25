package employee_management_system.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEmployeeRequest {
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email;
    @Pattern(regexp = "^\\d{10}$", message = "Invalid phone number")
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    private String department;
    private String designation;
    @NotNull(message = "Salary is required")
    @Positive(message = "Salary must be greater than 0")
    private BigDecimal salary;
    private LocalDate joiningDate;
}
