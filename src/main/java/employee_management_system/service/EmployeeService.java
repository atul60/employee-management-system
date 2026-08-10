package employee_management_system.service;

import java.util.*;
import org.springframework.stereotype.Service;

import employee_management_system.entity.Employee;
import employee_management_system.repository.EmployeeRepository;

@Service
public class EmployeeService {

    private EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public List<Employee> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        return employees;
    }
    
}
