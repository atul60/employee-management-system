package employee_management_system.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import employee_management_system.dto.request.CreateEmployeeRequest;
import employee_management_system.dto.request.EmployeeListQuery;
import employee_management_system.dto.request.ReportsRequest;
import employee_management_system.dto.request.Page.SearchedPageQuery;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.dto.response.PageResponse;
import employee_management_system.dto.response.ProjectResponse;
import employee_management_system.dto.response.ReportsResponse;
import employee_management_system.entity.Employee;
import employee_management_system.entity.ProjectAssignment;
import employee_management_system.entity.enums.StatusCode;
import employee_management_system.exception.ApiException;
import employee_management_system.mapper.EmployeeMapper;
import employee_management_system.mapper.ProjectMapper;
import employee_management_system.repository.DepartmentRepository;
import employee_management_system.repository.DesignationRepository;
import employee_management_system.repository.EmployeeRepository;
import employee_management_system.repository.LocationRepository;
import employee_management_system.repository.ProjectAssignmentRepository;

@Service
public class EmployeeService {

    private EmployeeRepository employeeRepository;
    private DepartmentRepository departmentRepository;
    private DesignationRepository designationRepository;
    private LocationRepository locationRepository;
    private EmployeeMapper employeeMapper;
    private ProjectMapper projectMapper;
    private ProjectAssignmentRepository projectAssignmentRepository;
    
    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository,
            LocationRepository locationRepository,
            ProjectAssignmentRepository projectAssignmentRepository,
            EmployeeMapper employeeMapper,
            ProjectMapper projectMapper
        ) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.locationRepository = locationRepository;
        this.projectAssignmentRepository = projectAssignmentRepository;
        this.employeeMapper = employeeMapper;
        this.projectMapper = projectMapper;
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
            throw new ApiException(StatusCode.DUPLICATE_EMAIL);
        }
        if (employeeRepository.existsByPhoneNumber(createEmployeeRequest.getPhoneNumber())) {
            throw new ApiException(StatusCode.DUPLICATE_PHONE, StatusCode.DUPLICATE_PHONE.getDefaultMessage());
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
                    .orElseThrow(() -> new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Department not found")));
        }
        if(createEmployeeRequest.getDesignationId() != null) {
            employee.setDesignation(designationRepository.findById(createEmployeeRequest.getDesignationId())
                    .orElseThrow(() -> new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Designation not found")));
        }
        if(createEmployeeRequest.getLocationId() != null) {
            employee.setLocation(locationRepository.findById(createEmployeeRequest.getLocationId())
                    .orElseThrow(() -> new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Location not found")));
        }
        // Request carries a Long id; entity association is Employee manager (column manager_id).
        if (createEmployeeRequest.getManagerId() != null) {
            employee.setManager(employeeRepository.findById(createEmployeeRequest.getManagerId())
                    .orElseThrow(() -> new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Manager not found")));
        }
        Employee createdEmployee = employeeRepository.save(employee);
        return employeeMapper.toResponse(createdEmployee);
    }

    public void deleteEmployeeById(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ApiException(StatusCode.RESOURCE_NOT_FOUND);
        }
        employeeRepository.deleteById(id);
    }

    public Employee updateEmployeeById(Long id, Employee employee) {
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new ApiException(StatusCode.RESOURCE_NOT_FOUND));
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

    /** Returns direct reports for a manager; 404 if that employee id does not exist. */
    public PageResponse<ReportsResponse> employeeReports(ReportsRequest reportsRequest) {
        Long managerId = reportsRequest.getManagerId();
        if (!employeeRepository.existsById(managerId)) {
            throw new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Manager not found");
        }
        String search = reportsRequest.getSearch();
        if (search != null) {
            search = search.isBlank() ? null : search.trim();
        }
        Page<Employee> employees = employeeRepository.findAllFilteredByManagerId(
                search,
                managerId,
                reportsRequest.toPageable());
        return PageResponse.from(employees.map(employeeMapper::toReportsResponse));
    }

    public PageResponse<ProjectResponse> employeeProjects(Long id, SearchedPageQuery pageQuery) {
        if (!employeeRepository.existsById(id)) {
            throw new ApiException(StatusCode.RESOURCE_NOT_FOUND, "Employee not found");
        }
        String search = pageQuery.getSearch();
        if (search != null) {
            search = search.isBlank() ? null : search.trim();
        }
        Page<ProjectAssignment> projectAssignments = projectAssignmentRepository.findAllFilteredByEmployeeId(
                search,
                id,
                pageQuery.toPageable());
        // Unwrap ProjectAssignment → Project, map to DTO, keep Page totals for PageResponse.
        return PageResponse.from(
                projectAssignments.map(assignment ->
                        projectMapper.toProjectResponse(assignment.getProject())));
    }
}
