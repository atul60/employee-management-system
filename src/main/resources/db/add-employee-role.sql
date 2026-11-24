-- One access role per employee. Safe to re-run the UPDATE; the ADD COLUMN is one-time.
-- ADMIN: Chief Executive Officer. HR: HR department, unless already ADMIN. Everyone else: EMPLOYEE.

ALTER TABLE employees
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE' AFTER status;

ALTER TABLE employees
    ADD CONSTRAINT chk_employees_role CHECK (role IN ('ADMIN', 'HR', 'EMPLOYEE'));

UPDATE employees e
LEFT JOIN designations g ON g.id = e.designation_id
LEFT JOIN departments d ON d.id = e.department_id
SET e.role = CASE
    WHEN g.title = 'Chief Executive Officer' THEN 'ADMIN'
    WHEN d.name = 'HR' THEN 'HR'
    ELSE 'EMPLOYEE'
END;
