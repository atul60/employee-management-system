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

    public Employee createEmployee(Employee employee) {
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException("Employee with this email already exists");
        }
        if(employeeRepository.existsByPhoneNumber(employee.getPhoneNumber())) {
            throw new RuntimeException("Employee with this phone number already exists");
        }
        return employeeRepository.save(employee);
    }

    public void deleteEmployeeById(Long id) {
        if(!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found");
        }
        employeeRepository.deleteById(id);
    }

    public Employee updateEmployeeById(Long id, Employee employee) {
        Employee existingEmployee = employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found"));
        existingEmployee.setFirstName(employee.getFirstName());
        existingEmployee.setLastName(employee.getLastName());
        existingEmployee.setEmail(employee.getEmail());
        existingEmployee.setPhoneNumber(employee.getPhoneNumber());
        existingEmployee.setDepartment(employee.getDepartment());
        existingEmployee.setDesignation(employee.getDesignation());
        existingEmployee.setSalary(employee.getSalary());
        existingEmployee.setJoiningDate(employee.getJoiningDate());
        existingEmployee.setStatus(employee.getStatus());
        return employeeRepository.save(existingEmployee);
    }
    
}
