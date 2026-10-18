<callout icon="➡️" color="gray_bg">
	**Path 52.5** · Previous: <mention-page url="https://app.notion.com/p/PLACEHOLDER_MANYTOONE"/> · Next: <mention-page url="https://app.notion.com/p/3db8511ff86f8145aa99e4d2bfb40bda"/>
</callout>
<callout icon="🎯" color="blue_bg">
	`@OneToMany` is the **opposite navigation** of `@ManyToOne`. EMS does not use it today. “Get employees by department” is still a one-to-many *business* relationship — implemented by querying the many side.
</callout>
## Overview
You noticed a puzzle: the business relationship is one department → many employees, yet `Department` has no `@OneToMany`, and the feature works. This note explains what `@OneToMany` actually is, when you would add it, and why EMS correctly stays unidirectional for now.
## Definition
`@OneToMany` on a parent entity means “this object has a collection of child entities.” Example you do **not** have yet:

```java
// on Department — optional, not in EMS today
@OneToMany(mappedBy = "department")
private List<Employee> employees;
```

`mappedBy = "department"` means: “do not create a second foreign key. Reuse the FK already owned by `Employee.department`.” The parent side is inverse; the child `@ManyToOne` owns the column.
## Why it exists
Sometimes application code wants to walk from parent to children in memory: `department.getEmployees()`. Cascades (“delete department and all its employees”) are another reason people add `@OneToMany`. The annotation exists so Hibernate can manage that collection and keep it consistent with the FK — not because SQL needs a list column on `departments`.
## What problem it would solve in EMS (and what problem you already solved without it)
**Solved without `@OneToMany`:** `DepartmentService` + `EmployeeRepository.findAllByDepartmentId` returns a **page** of employees for `GET /api/departments/{id}/employees`. That is the production-friendly way: filter + limit in SQL.

**What `@OneToMany` would add:** the ability to write `department.getEmployees()` in Java. That is convenient for small graphs, dangerous for Engineering with ~200 employees if you serialize the entity to JSON or load the collection by accident.
## How Spring Boot / JPA would use it
If you added the collection on `Department`:
1. Hibernate still uses `employees.department_id` as the only FK.
2. Accessing `getEmployees()` triggers a SELECT `FROM employees WHERE department_id = ?` (lazy by default for `@OneToMany`).
3. `mappedBy` prevents a join table or a second FK from being invented.
4. If you forgot `mappedBy` and put `@JoinColumn` on the wrong side, you can get a broken or duplicate mapping.

Spring Boot does not require `@OneToMany` for nested REST resources. Controllers call services; services call repositories. The URL `/departments/{id}/employees` is an API design choice, not proof that the parent entity must hold a list.
## Where it is used in real projects
Bidirectional mappings appear when domain logic truly needs both directions (for example aggregate roots that enforce invariants on children). Many teams still prefer **unidirectional `@ManyToOne` only**, and load children with repository queries plus paging. That matches EMS. Use `@OneToMany` when you have a clear in-memory reason — not “because the ER diagram says 1:N.”
## Mental model
`@ManyToOne` = sticky note on the employee (“I belong to dept 2”).
`@OneToMany` = optional roster printed on the department door (“here are my people”).
The sticky notes alone are enough to answer “who is in Engineering?” by reading every sticky that says 2. The roster is a convenience copy. If the roster is incomplete or huge, prefer reading sticky notes with a SQL filter (repository query) instead of photocopying the whole roster into RAM.
## Internal working (if you added it)
1. `Employee.department` remains `@ManyToOne` + `@JoinColumn` — **owning side**.
2. `Department.employees` gets `@OneToMany(mappedBy = "department")` — **inverse side**.
3. Hibernate syncs using the owning side’s FK when flushing.
4. Lazy load of `employees` runs a separate SELECT (classic N+1 risk if you do this per department in a loop).
5. Jackson serializing `Department` with a loaded `employees` list can dump hundreds of rows or recurse if `Employee` also embeds `Department` — another reason EMS maps to DTOs.
## Common mistakes
- Adding `@OneToMany` “because the relationship is 1:N” even when no code calls `getEmployees()`.
- Forgetting `mappedBy`, so Hibernate thinks there are two mappings and may create a join table or wrong column.
- Using `FetchType.EAGER` on `@OneToMany` — every department load pulls all employees.
- Returning `Department` entities from a REST controller with the collection initialized — unbounded JSON payload.
- Assuming empty `getEmployees()` means the department does not exist. Empty collection is not the same as a missing parent; validate with `existsById` for 404.
## Best practices
- Default for EMS: keep unidirectional `@ManyToOne` on `Employee`.
- Load children with `Page<Employee> findByDepartmentId(...)` (or your `findAllByDepartmentId`) and return `PageResponse`.
- Add `@OneToMany` only when a use case needs the collection in domain logic — and keep it lazy.
- Never expose the collection directly in API responses; map to DTOs.
- If you add both sides, treat the `@ManyToOne` side as the source of truth when setting relationships (`employee.setDepartment(dept)`), not only `dept.getEmployees().add(employee)` unless you maintain both carefully.
## Connection to concepts you already know
- Previous note `@ManyToOne` — owning side; learn that first.
- <mention-page url="https://app.notion.com/p/3bc8511ff86f8150a24cc2b56df41252"/> — 1:N in SQL does not require a list column on the parent table.
- <mention-page url="https://app.notion.com/p/3c08511ff86f81f8805ed1b5f23b2295"/> — lazy collections and N+1 show up in Hibernate SQL logs.
- Module 9 pagination notes — paging is why you query the many side instead of loading `List` on the parent.
## EMS code example (contrast)
**What EMS has (enough):**

```java
// Department.java — no employees collection
public class Department {
    private Long id;
    private String name;
}

// DepartmentService
employeeRepository.findAllByDepartmentId(search, id, pageQuery.toPageable())
    .map(employeeMapper::toResponse);
```

**What you would add only if needed later:**

```java
@OneToMany(mappedBy = "department")
private List<Employee> employees;
```

Until a real use case needs that list in Java, skip it. Your nested API already works.
## Quick self-check
1. Does a one-to-many *business* relationship require `@OneToMany` in JPA?
	- No. Unidirectional `@ManyToOne` on the child plus a query/filter is enough, and that is what EMS uses.
2. What does `mappedBy = "department"` mean?
	- The FK is already defined by `Employee.department`. The collection is inverse; do not create another column.
3. Why is paging plus `EmployeeRepository` safer than `department.getEmployees()` for Engineering?
	- `getEmployees()` can load all ~200 rows into memory at once; a `Page` limits SQL with LIMIT/OFFSET (or equivalent) and matches your API contract.
