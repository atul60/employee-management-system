package employee_management_system.service;

import java.util.*;
import org.springframework.stereotype.Service;

import employee_management_system.dto.request.CreateEmployeeRequest;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.entity.Employee;
import employee_management_system.exception.DuplicateResourceException;
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

    public EmployeeResponse createEmployee(CreateEmployeeRequest createEmployeeRequest) {
        if (employeeRepository.existsByEmail(createEmployeeRequest.getEmail())) {
            throw new DuplicateResourceException("Employee with this email already exists");
        }
        if(employeeRepository.existsByPhoneNumber(createEmployeeRequest.getPhoneNumber())) {
            throw new RuntimeException("Employee with this phone number already exists");
        }
        Employee employee = new Employee();
        employee.setFirstName(createEmployeeRequest.getFirstName());
        employee.setLastName(createEmployeeRequest.getLastName());
        employee.setEmail(createEmployeeRequest.getEmail());
        employee.setPhoneNumber(createEmployeeRequest.getPhoneNumber());
        employee.setDepartment(createEmployeeRequest.getDepartment());
        employee.setDesignation(createEmployeeRequest.getDesignation());
        employee.setSalary(createEmployeeRequest.getSalary());
        employee.setJoiningDate(createEmployeeRequest.getJoiningDate());
        
        Employee createdEmployee = employeeRepository.save(employee);
        EmployeeResponse employeeResponse = new EmployeeResponse();
        employeeResponse.setId(createdEmployee.getId());
        employeeResponse.setFirstName(createdEmployee.getFirstName());
        employeeResponse.setLastName(createdEmployee.getLastName());
        employeeResponse.setEmail(createdEmployee.getEmail());
        employeeResponse.setPhoneNumber(createdEmployee.getPhoneNumber());
        employeeResponse.setDepartment(createdEmployee.getDepartment());
        employeeResponse.setDesignation(createdEmployee.getDesignation());
        employeeResponse.setSalary(createdEmployee.getSalary());
        employeeResponse.setJoiningDate(createdEmployee.getJoiningDate());
        employeeResponse.setStatus(createdEmployee.getStatus());
        return employeeResponse;
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
