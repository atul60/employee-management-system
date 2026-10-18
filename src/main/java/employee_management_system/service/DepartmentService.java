package employee_management_system.service;

import org.springframework.stereotype.Service;

import employee_management_system.dto.request.DepartmentListQuery;
import employee_management_system.dto.response.DepartmentResponse;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.dto.response.PageResponse;
import employee_management_system.entity.Department;
import employee_management_system.mapper.EmployeeMapper;
import employee_management_system.repository.DepartmentRepository;
import employee_management_system.repository.EmployeeRepository;

@Service 
public class DepartmentService {

    private DepartmentRepository departmentRepository;
    private EmployeeRepository employeeRepository;
    private EmployeeMapper employeeMapper;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository, EmployeeMapper employeeMapper) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
    }

    public PageResponse<DepartmentResponse> getDepartmentsByQuery(DepartmentListQuery pageQuery) {
        return PageResponse.from(departmentRepository.findAllFiltered(pageQuery.getSearch(), pageQuery.toPageable()).map(this::toDepartmentResponse));
    } 

    public DepartmentResponse toDepartmentResponse(Department department) {
        DepartmentResponse response = new DepartmentResponse();
        response.setId(department.getId());
        response.setName(department.getName());
        return response;
    }

    public PageResponse<EmployeeResponse> getEmployeedByDepartmentId(Long id, DepartmentListQuery pageQuery) {
        return PageResponse.from(employeeRepository.findAllByDepartmentId(pageQuery.getSearch(), id, pageQuery.toPageable()).map(employeeMapper::toResponse));
    }
}
