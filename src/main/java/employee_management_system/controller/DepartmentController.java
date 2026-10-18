package employee_management_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import employee_management_system.dto.request.DepartmentListQuery;
import employee_management_system.dto.response.DepartmentResponse;
import employee_management_system.dto.response.EmployeeResponse;
import employee_management_system.dto.response.PageResponse;
import employee_management_system.service.DepartmentService;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/list")
    public PageResponse<DepartmentResponse> getDepartmentsByQuery(DepartmentListQuery pageQuery) {
        return departmentService.getDepartmentsByQuery(pageQuery);
    }

    @GetMapping("/{id}/employees")
    public PageResponse<EmployeeResponse> getDepartmentEmployees(@PathVariable Long id, DepartmentListQuery pageQuery) {
        return departmentService.getEmployeedByDepartmentId(id, pageQuery);
    }
    
    
}
