# Safe plan: multi-table EMS playground

**Goal:** Create the company-shaped MySQL schema and dummy data in one go, then learn Spring Boot (JPA, joins, DTOs, APIs) without Hibernate destroying the tables.

**Rule:** SQL owns the schema until every Java entity matches it. Spring owns the APIs after that.

Do not skip checkpoints. If a checkpoint fails, stop. Do not start the next phase.

---

## Why this order exists

You already have:

- MySQL database `employee_db`
- table `employees` with `department` and `designation` as **strings**
- `spring.jpa.hibernate.ddl-auto=update`

If you run the new SQL and then start the app **before** changing `Employee.java`, Hibernate will try to “fix” the table: it will add the old string columns back and ignore foreign keys.

Safe sequence:

1. Stop the app  
2. Backup  
3. Freeze Hibernate  
4. Run schema + seed  
5. Verify joins in MySQL  
6. Map Java **one relationship at a time**  
7. Turn Hibernate validation back on  

---

## Current vs target

| Now | Target |
|-----|--------|
| One table, string `department` / `designation` | 9 tables, foreign keys |
| `GET` employee returns a flat row | `GET` employee joins department, manager, location |
| Hibernate `update` invents columns | You decide columns in SQL first |

**Tables to create**

1. `locations`  
2. `designations`  
3. `departments` (`parent_id`, `location_id`, `head_id`)  
4. `employees` (FKs instead of strings)  
5. `employee_addresses`  
6. `projects`  
7. `project_assignments` (join table with role + allocation)  
8. `leave_types`  
9. `leave_requests`  

---

## Phase 0 — freeze the running app

**Problem:** a running Spring process with `ddl-auto=update` can alter tables while you run SQL.

1. Stop `EmployeeManagementSystemApplication` in the IDE terminal (Ctrl+C).  
2. Confirm nothing is listening on your API port (usually `8080`).  
3. Do **not** click Run again until Phase 2 is done.

**Checkpoint:** app is stopped. MySQL is still running.

---

## Phase 1 — backup (do not skip)

This script **drops** `employees`. Existing rows are gone unless you backup.

```bash
mysqldump -u root employee_db > ~/employee_db_backup_$(date +%Y%m%d).sql
```

If `mysqldump` asks for a password, press Enter (your app uses an empty password).

**Checkpoint:** the `.sql` backup file exists and is not empty.

To restore later:

```bash
mysql -u root employee_db < ~/employee_db_backup_YYYYMMDD.sql
```

---

## Phase 2 — freeze Hibernate so it cannot rewrite tables

In `src/main/resources/application.properties` change:

```properties
spring.jpa.hibernate.ddl-auto=update
```

to:

```properties
spring.jpa.hibernate.ddl-auto=none
```

| Value | Meaning |
|-------|---------|
| `update` | Hibernate adds/changes columns to match entities. **Unsafe** until entities match the new schema. |
| `none` | Hibernate does not touch schema. SQL file is the source of truth. |
| `validate` | Later: Hibernate checks entities match tables, then fails fast if they do not. |

Keep `none` through Phase 3 and the first Java mapping slices. Switch to `validate` only when entities and tables agree.

**Checkpoint:** the property is saved. App is still stopped.

---

## Phase 3 — create tables and dummy data

### 3.1 Connect

```bash
mysql -u root employee_db
```

Or paste the script into MySQL Workbench / TablePlus against `employee_db`.

### 3.2 Run the script

Copy **the entire script** in [Appendix A](#appendix-a--schema--seed-sql) and execute it.

What the script does, in order:

1. Drops child tables first (FK-safe)  
2. Creates parent tables first (`locations`, `designations`)  
3. Creates `departments` **without** `head_id` FK (circular with employees)  
4. Creates `employees`  
5. Adds `fk_departments_head`  
6. Creates addresses, projects, assignments, leave tables  
7. Inserts dummy rows **parents first**  
8. `UPDATE departments SET head_id = ...` after employees exist  

**Checkpoint:** the script finishes with no `ERROR`. The sanity `SELECT` at the end should show:

| table | count |
|-------|------:|
| locations | 3 |
| departments | 5 |
| employees | 10 |
| assignments | 7 |
| leave_requests | 4 |

### 3.3 Prove the joins work (in MySQL, not Spring)

```sql
SELECT e.first_name,
       d.name AS department,
       m.first_name AS manager,
       p.name AS project
FROM employees e
JOIN departments d ON d.id = e.department_id
LEFT JOIN employees m ON m.id = e.manager_id
LEFT JOIN project_assignments pa ON pa.employee_id = e.id
LEFT JOIN projects p ON p.id = pa.project_id
ORDER BY e.id;
```

You should see Atul in Engineering, manager Arjun, project Atlas.

**Checkpoint:** this query returns rows. If it fails, do not write Java yet.

---

## Phase 4 — do not start Spring yet

Your Java still looks like this:

```java
private String department;
private String designation;
```

The database now has `department_id` and `designation_id`.

If you start the app with `ddl-auto=none`, Hibernate will **not** drop FKs, but **create/update employee APIs will break** because the entity does not match the table.

**Allowed now:** inspect MySQL, write the first entity mappings.  
**Not allowed:** Run the app to “see if it works.”

---

## Phase 5 — Spring Boot in slices (this is the learning)

Map and expose **one relationship per slice**. Commit (or at least test) after each slice. Do not generate all entities + all controllers in one sitting.

After each slice you may start the app. Keep `ddl-auto=none` until Slice 1 entities match.

### Slice 1 — employee + department + designation + location

**New problem:** create/get employee must use IDs, not strings.

- Add entities: `Location`, `Department`, `Designation`  
- Change `Employee`: remove `String department` / `String designation`  
- Add `@ManyToOne` + `@JoinColumn` for `department`, `designation`, `location`  
- Keep `manager` **out** of this slice if you want a smaller first step; or add `manager_id` as `@ManyToOne` here (self-join)  
- Change `CreateEmployeeRequest` to `departmentId`, `designationId`, `locationId`  
- Service: `departmentRepository.findById(...)` or `ResourceNotFoundException`  
- `GET /employees/{id}` returns a DTO with nested names, **not** the entity (avoids JSON recursion)

**Checkpoint:** `GET` one seeded employee (id `4`, Atul) shows department `Engineering`. Old create body with `"department": "HR"` must fail validation or 400.

### Slice 2 — manager self-join + reportees

- `Employee.manager` → `@ManyToOne` `@JoinColumn(name = "manager_id")`  
- `GET /employees/{id}/reportees` → employees where `manager_id = id`  
- CEO (`Riya`, id `1`) has `manager_id` null — that must be allowed  

**Checkpoint:** reportees of Arjun (id `2`) include Atul, Dev, Meera.

### Slice 3 — addresses (one-to-many)

- `EmployeeAddress` entity, `employee_id` FK  
- `GET /employees/{id}/addresses`  
- Do not dump addresses inside every employee list (N+1 and payload size)

**Checkpoint:** Atul (id `4`) has CURRENT + PERMANENT addresses.

### Slice 4 — projects + assignments (join entity)

- Do **not** use `@ManyToMany`  
- `Project` + `ProjectAssignment` (role, `allocationPercent`, dates)  
- `GET /projects/{id}/members`  
- `GET /employees/{id}/projects`  

**Checkpoint:** Atlas members include Arjun as `LEAD` and Atul as `MEMBER`.

### Slice 5 — leave workflow

- `LeaveType`, `LeaveRequest`  
- `POST /leave-requests` → `PENDING`  
- `POST /leave-requests/{id}/approve` → only manager, set `reviewed_by`  
- Reuse `StatusCode` + `GlobalExceptionHandler` for illegal transitions  

**Checkpoint:** Atul’s casual leave stays `PENDING`; Meera’s sick leave is `APPROVED` by Arjun.

### After Slice 1 is stable

Change:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Now Hibernate **checks** mappings against MySQL and refuses to start if they drift. It still does not invent columns.

---

## Phase 6 — safety rules (keep these)

1. **Never** set `ddl-auto=update` again while entities and SQL disagree.  
2. **Never** keep both `department` (string) and `department_id` on `employees`. One source of truth.  
3. **Never** return JPA entities with both sides of a relationship mapped (`employee.department.employees...`). Use DTOs.  
4. **Never** insert `departments.head_id` before the employee row exists.  
5. Seed data is a playground. New APIs should still validate IDs and uniqueness the same way you do for email today.  
6. Do not add payroll, auth, or attendance until these five slices work.

---

## Rollback

If SQL went wrong and you have the Phase 1 dump:

```bash
mysql -u root employee_db < ~/employee_db_backup_YYYYMMDD.sql
```

Then set `ddl-auto=update` only if you are returning to the **old** single-table `Employee` entity.

If Java mappings went wrong but SQL is fine: revert Java only. Leave MySQL as the playground.

---

## What “done” looks like

- MySQL has 9 tables and dummy rows you can join  
- Spring maps those tables; it does not recreate a string `department`  
- You can explain each API as: **which tables, which FK, which DTO**  
- Create employee accepts IDs, not free-text department names  

---

## Appendix A — schema + seed SQL

```sql
USE employee_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS leave_requests;
DROP TABLE IF EXISTS project_assignments;
DROP TABLE IF EXISTS employee_addresses;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS designations;
DROP TABLE IF EXISTS leave_types;
DROP TABLE IF EXISTS locations;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE locations (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    name    VARCHAR(100) NOT NULL,
    city    VARCHAR(80)  NOT NULL,
    country VARCHAR(80)  NOT NULL
);

CREATE TABLE designations (
    id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL UNIQUE,
    level VARCHAR(10)  NOT NULL
);

CREATE TABLE departments (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL UNIQUE,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    parent_id   BIGINT NULL,
    location_id BIGINT NOT NULL,
    head_id     BIGINT NULL,
    CONSTRAINT fk_departments_parent
        FOREIGN KEY (parent_id) REFERENCES departments(id),
    CONSTRAINT fk_departments_location
        FOREIGN KEY (location_id) REFERENCES locations(id)
);

CREATE TABLE employees (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    first_name     VARCHAR(50)  NOT NULL,
    last_name      VARCHAR(50)  NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    phone_number   VARCHAR(10)  NOT NULL UNIQUE,
    salary         DECIMAL(12,2) NOT NULL,
    joining_date   DATE NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    department_id  BIGINT NOT NULL,
    designation_id BIGINT NOT NULL,
    location_id    BIGINT NOT NULL,
    manager_id     BIGINT NULL,
    CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_employees_designation
        FOREIGN KEY (designation_id) REFERENCES designations(id),
    CONSTRAINT fk_employees_location
        FOREIGN KEY (location_id) REFERENCES locations(id),
    CONSTRAINT fk_employees_manager
        FOREIGN KEY (manager_id) REFERENCES employees(id)
);

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_head
        FOREIGN KEY (head_id) REFERENCES employees(id);

CREATE TABLE employee_addresses (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    type        VARCHAR(20) NOT NULL,
    line1       VARCHAR(200) NOT NULL,
    city        VARCHAR(80)  NOT NULL,
    state       VARCHAR(80)  NOT NULL,
    pincode     VARCHAR(10)  NOT NULL,
    CONSTRAINT fk_addresses_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE projects (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(120) NOT NULL,
    code          VARCHAR(20)  NOT NULL UNIQUE,
    department_id BIGINT NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE NULL,
    status        VARCHAR(20) NOT NULL,
    CONSTRAINT fk_projects_department
        FOREIGN KEY (department_id) REFERENCES departments(id)
);

CREATE TABLE project_assignments (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id         BIGINT NOT NULL,
    project_id          BIGINT NOT NULL,
    role                VARCHAR(20) NOT NULL,
    allocation_percent  INT NOT NULL,
    start_date          DATE NOT NULL,
    end_date            DATE NULL,
    CONSTRAINT fk_assignments_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_assignments_project
        FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT uq_assignment_active
        UNIQUE (employee_id, project_id)
);

CREATE TABLE leave_types (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    name                VARCHAR(50) NOT NULL UNIQUE,
    max_days_per_year   INT NOT NULL
);

CREATE TABLE leave_requests (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id   BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE NOT NULL,
    status        VARCHAR(20) NOT NULL,
    reviewed_by   BIGINT NULL,
    CONSTRAINT fk_leave_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_leave_type
        FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT fk_leave_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES employees(id)
);

INSERT INTO locations (id, name, city, country) VALUES
(1, 'Bengaluru HQ', 'Bengaluru', 'India'),
(2, 'Pune Office',  'Pune',      'India'),
(3, 'Remote',       'Remote',    'India');

INSERT INTO designations (id, title, level) VALUES
(1, 'Chief Executive Officer', 'L0'),
(2, 'Engineering Manager',     'L4'),
(3, 'Senior Software Engineer','L3'),
(4, 'Software Engineer',       'L2'),
(5, 'HR Manager',              'L4'),
(6, 'HR Executive',            'L1'),
(7, 'Accountant',              'L2');

INSERT INTO departments (id, name, code, parent_id, location_id, head_id) VALUES
(1, 'Corporate',    'CORP', NULL, 1, NULL),
(2, 'Engineering',  'ENG',  1,    1, NULL),
(3, 'HR',           'HR',   1,    1, NULL),
(4, 'Finance',      'FIN',  1,    2, NULL),
(5, 'Platform',     'PLT',  2,    1, NULL);

INSERT INTO employees
(id, first_name, last_name, email, phone_number, salary, joining_date, status,
 department_id, designation_id, location_id, manager_id) VALUES
(1, 'Riya',  'Sharma', 'riya.sharma@ems.local',  '9811111111', 2500000, '2018-04-01', 'ACTIVE',   1, 1, 1, NULL),
(2, 'Arjun', 'Mehta',  'arjun.mehta@ems.local',  '9822222222', 1800000, '2019-06-15', 'ACTIVE',   2, 2, 1, 1),
(3, 'Priya', 'Nair',   'priya.nair@ems.local',   '9833333333', 1600000, '2019-08-01', 'ACTIVE',   3, 5, 1, 1),
(4, 'Atul',  'Kumar',  'atul.kumar@ems.local',   '9844444444', 1200000, '2022-01-10', 'ACTIVE',   2, 4, 1, 2),
(5, 'Dev',   'Patel',  'dev.patel@ems.local',    '9855555555', 1500000, '2020-11-02', 'ACTIVE',   5, 3, 1, 2),
(6, 'Sneha', 'Iyer',   'sneha.iyer@ems.local',   '9866666666',  700000, '2023-03-20', 'ACTIVE',   3, 6, 1, 3),
(7, 'Kabir', 'Shah',   'kabir.shah@ems.local',   '9877777777',  900000, '2021-07-12', 'ACTIVE',   4, 7, 2, 1),
(8, 'Meera', 'Joshi',  'meera.joshi@ems.local',  '9888888888', 1100000, '2022-09-05', 'ACTIVE',   2, 4, 2, 2),
(9, 'Vikram','Rao',    'vikram.rao@ems.local',   '9899999999', 1000000, '2020-02-14', 'ON_LEAVE', 5, 4, 3, 5),
(10,'Anita', 'Das',    'anita.das@ems.local',    '9800000000',  800000, '2017-05-30', 'INACTIVE', 4, 7, 2, 1);

UPDATE departments SET head_id = 1 WHERE id = 1;
UPDATE departments SET head_id = 2 WHERE id = 2;
UPDATE departments SET head_id = 3 WHERE id = 3;
UPDATE departments SET head_id = 7 WHERE id = 4;
UPDATE departments SET head_id = 5 WHERE id = 5;

INSERT INTO employee_addresses (employee_id, type, line1, city, state, pincode) VALUES
(4, 'CURRENT',   '12 MG Road',           'Bengaluru', 'Karnataka', '560001'),
(4, 'PERMANENT', '45 Lake View',         'Patna',     'Bihar',     '800001'),
(8, 'CURRENT',   '88 FC Road',           'Pune',      'Maharashtra','411004'),
(2, 'CURRENT',   '3 Koramangala 5th Blk','Bengaluru', 'Karnataka', '560095');

INSERT INTO projects (id, name, code, department_id, start_date, end_date, status) VALUES
(1, 'Project Atlas',     'ATL', 2, '2025-01-15', NULL,         'ACTIVE'),
(2, 'Payroll Revamp',    'PAY', 4, '2024-08-01', '2026-03-31', 'ACTIVE'),
(3, 'People Hub',        'HUB', 3, '2024-02-01', '2025-12-31', 'COMPLETED');

INSERT INTO project_assignments
(employee_id, project_id, role, allocation_percent, start_date, end_date) VALUES
(2, 1, 'LEAD',   50,  '2025-01-15', NULL),
(4, 1, 'MEMBER', 100, '2025-01-15', NULL),
(5, 1, 'MEMBER', 80,  '2025-02-01', NULL),
(8, 1, 'MEMBER', 50,  '2025-03-01', NULL),
(7, 2, 'LEAD',   100, '2024-08-01', NULL),
(3, 3, 'LEAD',   40,  '2024-02-01', '2025-12-31'),
(6, 3, 'MEMBER', 100, '2024-02-01', '2025-12-31');

INSERT INTO leave_types (id, name, max_days_per_year) VALUES
(1, 'Casual',    12),
(2, 'Sick',      10),
(3, 'Privilege', 15);

INSERT INTO leave_requests
(employee_id, leave_type_id, start_date, end_date, status, reviewed_by) VALUES
(4, 1, '2026-09-01', '2026-09-03', 'PENDING',  NULL),
(8, 2, '2026-08-10', '2026-08-11', 'APPROVED', 2),
(9, 3, '2026-08-20', '2026-08-29', 'APPROVED', 5),
(6, 1, '2026-07-04', '2026-07-05', 'REJECTED', 3);

SELECT 'locations' t, COUNT(*) n FROM locations
UNION ALL SELECT 'departments', COUNT(*) FROM departments
UNION ALL SELECT 'employees', COUNT(*) FROM employees
UNION ALL SELECT 'assignments', COUNT(*) FROM project_assignments
UNION ALL SELECT 'leave_requests', COUNT(*) FROM leave_requests;
```

---

## Appendix B — dummy data cheat sheet

| id | name | department | manager | useful for |
|----|------|------------|---------|------------|
| 1 | Riya Sharma | Corporate | — | CEO, no manager |
| 2 | Arjun Mehta | Engineering | Riya | Atlas lead, Meera’s leave approver |
| 3 | Priya Nair | HR | Riya | People Hub lead |
| 4 | Atul Kumar | Engineering | Arjun | two addresses, pending leave |
| 5 | Dev Patel | Platform | Arjun | Platform head |
| 8 | Meera Joshi | Engineering (Pune) | Arjun | approved sick leave |

---

## Next action

Start at **Phase 0**. When Phase 3 checkpoint passes, tell your mentor (or Agent mode) to begin **Slice 1 only**.
