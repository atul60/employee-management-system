package employee_management_system.mapper;

import org.springframework.stereotype.Component;

import employee_management_system.dto.response.DepartmentResponse;
import employee_management_system.dto.response.DesignationResponse;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.entity.Department;
import employee_management_system.entity.Designation;
import employee_management_system.entity.Employee;

@Component 
public class EmployeeMapper {
    public EmployeeResponse toResponse(Employee employee) {
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

    public DepartmentResponse toDepartmentResponse(Department department) {
        if (department == null) {
            return null;
        }
        DepartmentResponse response = new DepartmentResponse();
        response.setId(department.getId());
        response.setName(department.getName());
        return response;
    }

    public DesignationResponse toDesignationResponse(Designation designation) {
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
