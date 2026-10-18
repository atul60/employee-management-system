<callout icon="➡️" color="gray_bg">
	**Path 52** · Previous: <mention-page url="https://app.notion.com/p/3c18511ff86f81e08f2ae58779dbfcec"/> · Next: <mention-page url="https://app.notion.com/p/PLACEHOLDER_ONETOMANY"/>
</callout>
<callout icon="🎯" color="blue_bg">
	`@ManyToOne` means many employees point at one department. The foreign key lives on the **employee** row (`department_id`). You do not need `@OneToMany` on `Department` for this to work.
</callout>
## Overview
In EMS, an employee belongs to one department, one designation, one location, and optionally one manager. In the database that is a foreign key column on `employees`. In Java, JPA expresses that with `@ManyToOne` plus `@JoinColumn` on the `Employee` entity. This note is about that many-to-one direction — the direction EMS already uses.
## Definition
`@ManyToOne` tells JPA: “this field is a reference to another entity, and many rows of this entity may share the same target.” On `Employee`, `private Department department` with `@ManyToOne` means many employees can share Engineering. Hibernate stores that link as a foreign key column (usually `department_id`), not by embedding the whole `Department` object inside the employee row.
## Why it exists
Relational databases store links as keys, not nested objects. Early EMS might have stored department as a string on the employee. That breaks when HR renames “Engineering”, when you need to list every person in a department, or when you need referential integrity. `@ManyToOne` is the JPA way to say “this Java field is that foreign key relationship,” so Hibernate can load `employee.getDepartment().getName()` and write `department_id` on insert/update without you writing JOIN SQL by hand every time.
## What problem it solves in EMS
Your create API accepts `departmentId`. `EmployeeService` loads a `Department` and sets it on the employee:

```java
employee.setDepartment(
    departmentRepository.findById(departmentId)
        .orElseThrow(...)
);
employeeRepository.save(employee);
```

Without `@ManyToOne`, you would either keep a raw `Long departmentId` and lose navigation, or keep a string name and lose a real FK. With `@ManyToOne`, the same field is both “object graph in Java” and “foreign key in MySQL.” Your list filter `e.department.id = :departmentId` in JPQL also depends on this mapping.
## How Spring Boot / JPA uses it
Spring Boot does not invent `@ManyToOne`. It comes from Jakarta Persistence. Hibernate (the provider behind `spring-boot-starter-data-jpa`) reads the annotation at startup, builds a metamodel, and generates SQL that reads/writes `department_id`. `@JoinColumn(name = "department_id")` names the actual MySQL column. If you omit `@JoinColumn`, Hibernate still creates a default column name (often `department_id`), but in EMS you name it explicitly because the schema already exists (`ddl-auto=none`).
## Where it is used in real projects
Almost every business system: order → customer, ticket → assignee, invoice → company, comment → author. Teams prefer putting `@ManyToOne` on the side that owns the FK (the “many” table). That matches how SQL is designed and keeps inserts simple: one row, one FK value.
## Mental model
Think of a sticky note on each employee badge: “Department #2.” Many badges can say #2. The Engineering team board does not need a handwritten list of every badge for the badge system to work — the badges already point at Engineering. `@ManyToOne` is the sticky note. `@OneToMany` would be an optional roster on the team board.
```plain text
departments (1)          employees (many)
id=2 Engineering   <---  department_id=2  (FK)
                     <---  department_id=2
                     <---  department_id=2
```
## Internal working
1. You annotate `Employee.department` with `@ManyToOne` and `@JoinColumn(name = "department_id")`.
2. On startup, Hibernate maps that field to column `employees.department_id` referencing `departments.id`.
3. On `save`, if `employee.getDepartment()` is a managed `Department` with `id = 2`, Hibernate writes `department_id = 2` in the INSERT/UPDATE.
4. On load, Hibernate may leave `department` as a lazy proxy. When you access `getDepartment().getName()`, Hibernate runs a SELECT on `departments` if needed.
5. In JPQL `WHERE e.department.id = :departmentId`, Hibernate translates to SQL on the FK column — no `@OneToMany` required.
6. Your API `GET /api/departments/{id}/employees` queries from `EmployeeRepository.findAllByDepartmentId` using that same FK filter, then maps with `EmployeeMapper`.
## Common mistakes
- Believing you must add `@OneToMany` on `Department` for “get employees by department” to work. EMS proves you do not: query the many side.
- Setting `@JoinColumn(nullable = true)` in Java while MySQL has `department_id NOT NULL`. Java allows null; MySQL rejects the INSERT (`Column 'department_id' cannot be null`). Align entity and schema.
- Sending JSON `"department": "Product Development"` while the DTO expects `departmentId`. Jackson ignores unknown fields; FK stays null; MySQL fails.
- Returning the `Employee` entity with a lazily loaded `department` outside a transaction → `LazyInitializationException`. Map to `EmployeeResponse` in the service (as `EmployeeMapper` does).
- Putting `@ManyToOne` on the DTO. DTOs are API shapes; entities own persistence annotations.
## Best practices
- Own the FK on the many side with unidirectional `@ManyToOne` unless you truly need `department.getEmployees()` in Java.
- Always pair with an explicit `@JoinColumn` when the DB schema already exists.
- Validate the target exists in the service before save (`departmentRepository.findById`).
- Keep list endpoints paged: never load “all employees of Engineering” into a collection on `Department` for JSON.
- Prefer filtering with repository/JPQL (`e.department.id = :id`) over navigating a huge `@OneToMany` list.
## Connection to concepts you already know
- <mention-page url="https://app.notion.com/p/3bc8511ff86f81b5a4ede3d02baa1b0a"/> — the SQL idea; `@ManyToOne` is the JPA annotation for that FK.
- <mention-page url="https://app.notion.com/p/3bc8511ff86f8150a24cc2b56df41252"/> — table-level story; this note is the Java mapping for one direction.
- <mention-page url="https://app.notion.com/p/3c08511ff86f81dca1c8fcb334b301d9"/> — annotations live in the JPA layer; Hibernate executes them.
- <mention-page url="https://app.notion.com/p/3db8511ff86f81ada90cded50e149468"/> — `e.department.id` in queries only works because of this mapping.
## EMS code example
From `Employee.java`:

```java
@ManyToOne
@JoinColumn(name = "department_id", nullable = true)
private Department department;

@ManyToOne
@JoinColumn(name = "designation_id", nullable = true)
private Designation designation;
```

From `EmployeeRepository` (department filter without `@OneToMany`):

```java
@Query("""
    SELECT e FROM Employee e
    WHERE (:departmentId IS NULL OR e.department.id = :departmentId)
    ...
    """)
Page<Employee> findAllFiltered(...);
```

`Department.java` has no `List<Employee>` — and `DepartmentService.getEmployeedByDepartmentId` still returns a page of employees.
## Quick self-check
1. Where does MySQL store the employee–department link — on `departments` or on `employees`?
	- On `employees.department_id`. The many side holds the FK.
2. Why can `GET /api/departments/2/employees` work with only `@ManyToOne` on `Employee`?
	- The query starts from `Employee` and filters by `e.department.id`. Navigation from department to a collection is optional.
3. You set `nullable = true` in `@JoinColumn` but MySQL says `department_id` cannot be null. Who wins, and why?
	- MySQL wins at INSERT time. `ddl-auto=none` means the annotation does not change the table; the real NOT NULL constraint still applies.
