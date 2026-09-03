package employee_management_system.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import employee_management_system.dto.request.CreateEmployeeRequest;
import employee_management_system.dto.response.DepartmentResponse;
import employee_management_system.dto.response.DesignationResponse;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.entity.Department;
import employee_management_system.entity.Designation;
import employee_management_system.entity.Employee;
import employee_management_system.entity.enums.StatusCode;
import employee_management_system.exception.DuplicateResourceException;
import employee_management_system.exception.ResourceNotFoundException;
import employee_management_system.repository.DepartmentRepository;
import employee_management_system.repository.DesignationRepository;
import employee_management_system.repository.EmployeeRepository;

@Service
public class EmployeeService {

    private EmployeeRepository employeeRepository;
    private DepartmentRepository departmentRepository;
    private DesignationRepository designationRepository;
    
    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EmployeeResponse createEmployee(CreateEmployeeRequest createEmployeeRequest) {
        if (employeeRepository.existsByEmail(createEmployeeRequest.getEmail())) {
            throw new DuplicateResourceException(StatusCode.DUPLICATE_EMAIL);
        }
        if (employeeRepository.existsByPhoneNumber(createEmployeeRequest.getPhoneNumber())) {
            throw new DuplicateResourceException(StatusCode.DUPLICATE_PHONE, StatusCode.DUPLICATE_PHONE.getDefaultMessage());
        }
        Employee employee = new Employee();
        employee.setFirstName(createEmployeeRequest.getFirstName());
        employee.setLastName(createEmployeeRequest.getLastName());
        employee.setEmail(createEmployeeRequest.getEmail());
        employee.setPhoneNumber(createEmployeeRequest.getPhoneNumber());
        employee.setDepartment(departmentRepository.findById(createEmployeeRequest.getDepartment())
                .orElseThrow(() -> new ResourceNotFoundException(StatusCode.EMPLOYEE_NOT_FOUND, "Department not found")));
        employee.setDesignation(designationRepository.findById(createEmployeeRequest.getDesignation())
                .orElseThrow(() -> new ResourceNotFoundException(StatusCode.EMPLOYEE_NOT_FOUND, "Designation not found")));
        employee.setSalary(createEmployeeRequest.getSalary());
        employee.setJoiningDate(createEmployeeRequest.getJoiningDate());
        
        Employee createdEmployee = employeeRepository.save(employee);
        return toResponse(createdEmployee);
    }

    public void deleteEmployeeById(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException(StatusCode.EMPLOYEE_NOT_FOUND);
        }
        employeeRepository.deleteById(id);
    }

    public Employee updateEmployeeById(Long id, Employee employee) {
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(StatusCode.EMPLOYEE_NOT_FOUND));
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

    private EmployeeResponse toResponse(Employee employee) {
        EmployeeResponse employeeResponse = new EmployeeResponse();
        employeeResponse.setId(employee.getId());
        employeeResponse.setFirstName(employee.getFirstName());
        employeeResponse.setLastName(employee.getLastName());
        employeeResponse.setEmail(employee.getEmail());
        employeeResponse.setPhoneNumber(employee.getPhoneNumber());
        employeeResponse.setDepartment(toDepartmentResponse(employee.getDepartment()));
        employeeResponse.setDesignation(toDesignationResponse(employee.getDesignation()));
        employeeResponse.setSalary(employee.getSalary());
        employeeResponse.setJoiningDate(employee.getJoiningDate());
        employeeResponse.setStatus(employee.getStatus());
        return employeeResponse;
    }

    private DepartmentResponse toDepartmentResponse(Department department) {
        if (department == null) {
            return null;
        }
        DepartmentResponse response = new DepartmentResponse();
        response.setId(department.getId());
        response.setName(department.getName());
        return response;
    }

    private DesignationResponse toDesignationResponse(Designation designation) {
        if (designation == null) {
            return null;
        }
        DesignationResponse response = new DesignationResponse();
        response.setId(designation.getId());
        response.setTitle(designation.getTitle());
        response.setLevel(designation.getLevel());
        return response;
    }
}
