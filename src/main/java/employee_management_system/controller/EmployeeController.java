package employee_management_system.controller;

import java.util.*;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import employee_management_system.dto.request.CreateEmployeeRequest;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.entity.Employee;
import employee_management_system.service.EmployeeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/list")
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @PostMapping("/create")
    public EmployeeResponse createEmployee( @Valid @RequestBody CreateEmployeeRequest createEmployeeRequest) {
        return employeeService.createEmployee(createEmployeeRequest);
    }

    @PostMapping("/update/{id}")
    public Employee updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        return employeeService.updateEmployeeById(id, employee);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployeeById(id);
    }
}