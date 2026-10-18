package employee_management_system.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import employee_management_system.dto.request.CreateEmployeeRequest;
import employee_management_system.dto.request.EmployeeListQuery;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.dto.response.PageResponse;
import employee_management_system.entity.Employee;
import employee_management_system.entity.enums.StatusCode;
import employee_management_system.exception.DuplicateResourceException;
import employee_management_system.exception.ResourceNotFoundException;
import employee_management_system.mapper.EmployeeMapper;
import employee_management_system.repository.DepartmentRepository;
import employee_management_system.repository.DesignationRepository;
import employee_management_system.repository.EmployeeRepository;
import employee_management_system.repository.LocationRepository;

@Service
public class EmployeeService {

    private EmployeeRepository employeeRepository;
    private DepartmentRepository departmentRepository;
    private DesignationRepository designationRepository;
    private LocationRepository locationRepository;
    private EmployeeMapper employeeMapper;
    
    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository,
            LocationRepository locationRepository,
            EmployeeMapper employeeMapper
        ) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.locationRepository = locationRepository;
        this.employeeMapper = employeeMapper;
    }

    public PageResponse<EmployeeResponse> getAllEmployees(EmployeeListQuery pageQuery) {
        String search = pageQuery.getSearch();
        if (search != null) {
            search = search.isBlank() ? null : search.trim();
        }
        Page<Employee> employees = employeeRepository.findAllFiltered(
                search,
                pageQuery.getStatus(),
                pageQuery.getDepartmentId(),
                pageQuery.getDesignationId(),
                pageQuery.toPageable());
        return PageResponse.from(employees.map(employeeMapper::toResponse));
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
        employee.setSalary(createEmployeeRequest.getSalary());
        employee.setJoiningDate(createEmployeeRequest.getJoiningDate());
        if(createEmployeeRequest.getDepartmentId() != null) {
            employee.setDepartment(departmentRepository.findById(createEmployeeRequest.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(StatusCode.EMPLOYEE_NOT_FOUND, "Department not found")));
        }
        if(createEmployeeRequest.getDesignationId() != null) {
            employee.setDesignation(designationRepository.findById(createEmployeeRequest.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException(StatusCode.EMPLOYEE_NOT_FOUND, "Designation not found")));
        }
        if(createEmployeeRequest.getLocationId() != null) {
            employee.setLocation(locationRepository.findById(createEmployeeRequest.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException(StatusCode.EMPLOYEE_NOT_FOUND, "Location not found")));
        }
        if(createEmployeeRequest.getManagerId() != null) {
            employee.setManagerId(employeeRepository.findById(createEmployeeRequest.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException(StatusCode.EMPLOYEE_NOT_FOUND, "Manager not found")));
        }
        Employee createdEmployee = employeeRepository.save(employee);
        return employeeMapper.toResponse(createdEmployee);
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
}
