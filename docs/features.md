# EMS Feature Plans

Learning-oriented plans for Employee Management System features.  
**Rule:** understand the problem → choose an approach → implement layer by layer → verify → reflect.

Do not skip checkpoints. If a checkpoint fails, stop and fix before moving on.

---

## Feature: Get employees by department

### Goal

Return employees that belong to one department, in a way that is clear to API clients, safe when the department does not exist, and consistent with the paging / DTO style you already use.

### Why this feature exists

In a real company system, managers and HR rarely want “all 1010 employees.” They ask:

- “Who works in Engineering?”
- “Show me HR employees, page 2.”

The database already models this with `employees.department_id → departments.id`. The feature is about exposing that relationship cleanly through the API.

### What you already have (important discovery)

You already filter by department on the list API:

| Piece | Location | Behavior |
|-------|----------|----------|
| Query param | `EmployeeListQuery.departmentId` | Optional filter |
| JPQL | `EmployeeRepository.findAllFiltered` | `(:departmentId IS NULL OR e.department.id = :departmentId)` |
| Endpoint | `GET /api/employees/list?departmentId=2` | Returns a page of employees |

So this feature is **not** “start from zero.” It is either:

1. **Reuse** the existing list filter and document it as the official way, or  
2. **Add a dedicated, resource-oriented endpoint** such as `GET /api/departments/{departmentId}/employees`.

Both are valid. They solve different API-design problems.

### Problem to articulate first

Answer these before writing code:

1. Should the client send a **query param** (`?departmentId=2`) or a **path variable** (`/departments/2/employees`)?
2. What should happen if `departmentId = 999` does not exist — empty page, or `404 DEPARTMENT_NOT_FOUND`?
3. Should the response reuse `PageResponse<EmployeeResponse>`?
4. Do you need status / search on this endpoint too, or only department?

Write your answers in a short note before Step 1.

---

### Recommended approach for this project

**Add a dedicated department-scoped endpoint**, and keep the existing list filter.

| Approach | When it fits | Trade-off |
|----------|--------------|-----------|
| Reuse `GET /api/employees/list?departmentId=` | Admin search screens with many filters | Department is “just another filter”; missing department id quietly returns everyone or empty results depending on usage |
| Dedicated `GET /api/departments/{id}/employees` | “Employees of this department” as a first-class use case | Clearer URL; you can validate the department exists and return `404` |

For learning Spring Boot path variables, nested resources, and not-found handling, the dedicated endpoint teaches more.

**Target API**

```http
GET /api/departments/{departmentId}/employees?pageNumber=1&pageSize=10&sortBy=firstName&sortDirection=asc
```

**Success response:** `PageResponse<EmployeeResponse>` (same shape as list).  
**Failure:** `404` with your existing `ErrorResponse` when the department id does not exist.

---

### Step-by-step plan

#### Step 0 — Confirm the data model (read only)

**Do**

1. Open `Employee.java` and confirm `@ManyToOne` → `Department` with join column `department_id`.
2. Open `Department.java` and note fields (`id`, `name`).
3. In MySQL, run:

```sql
SELECT d.id, d.name, COUNT(e.id) AS employee_count
FROM departments d
LEFT JOIN employees e ON e.department_id = d.id
GROUP BY d.id, d.name
ORDER BY d.id;
```

**Checkpoint**

- You know which department ids exist (e.g. Engineering = 2) and roughly how many employees each has.
- You can explain why filtering must use `department_id`, not the department name string.

---

#### Step 1 — Decide contract (no code yet)

**Do**

Write down:

| Decision | Your choice |
|----------|-------------|
| HTTP method | `GET` |
| URL | `/api/departments/{departmentId}/employees` |
| Path variable type | `Long departmentId` |
| Query params | Reuse `PageQuery` (`pageNumber`, `pageSize`, `sortBy`, `sortDirection`) |
| Response body | `PageResponse<EmployeeResponse>` |
| Unknown department | `404` + `StatusCode` (add `DEPARTMENT_NOT_FOUND` if missing) |

**Checkpoint**

- Contract fits the style of `/api/employees/list` (paged DTO, not raw `Employee` entities).
- You are not returning `List<Employee>` entities directly from the controller.

---

#### Step 2 — StatusCode for missing department

**Do**

1. Open `StatusCode.java`.
2. Add something like `DEPARTMENT_NOT_FOUND` with `HttpStatus.NOT_FOUND` and a clear default message.
3. Decide whether service will throw `ResourceNotFoundException` with that code.

**Why**

Today create/update reuse `EMPLOYEE_NOT_FOUND` for missing department. A dedicated feature is the right moment to make the business code honest.

**Checkpoint**

- `StatusCode` compiles.
- You can say out loud: “404 means the department resource does not exist; empty page means the department exists but has zero employees.”

---

#### Step 3 — Repository query (narrow, department-required)

**Do**

In `EmployeeRepository`, add a method that **requires** a department id (not optional like `findAllFiltered`):

Option A — derived query:

```java
Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);
```

Option B — explicit JPQL:

```java
@Query("""
        SELECT e FROM Employee e
        WHERE e.department.id = :departmentId
        """)
Page<Employee> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);
```

Prefer Option A first if Spring Data naming is enough. Use Option B if you later add joins or extra conditions.

**Checkpoint**

- Method name / JPQL clearly means “only this department.”
- Return type is `Page<Employee>`, not `List<Employee>`, so paging stays at the database.

---

#### Step 4 — Service method

**Do**

In `EmployeeService` (or a new `DepartmentService` if you want department ownership clearer):

1. Accept `Long departmentId` + `PageQuery`.
2. Check department exists via `DepartmentRepository.findById` / `existsById`.
3. If missing → throw `ResourceNotFoundException(StatusCode.DEPARTMENT_NOT_FOUND)`.
4. If present → call `employeeRepository.findByDepartmentId(departmentId, pageQuery.toPageable())`.
5. Map `Page<Employee>` → `PageResponse<EmployeeResponse>` using your existing `toResponse` mapper.

**Why this order**

Validate the parent resource first. Then load children. That gives a clear 404 vs empty-page distinction.

**Checkpoint**

- Service never returns a raw entity page.
- Missing department throws before the employee query (or at least before mapping a silent empty result that hides the bug).

---

#### Step 5 — Controller endpoint

**Do**

Prefer a `DepartmentController` (keeps employee controller from owning every nested route):

```text
@RestController
@RequestMapping("/api/departments")
class DepartmentController

GET /{departmentId}/employees
```

Wire:

1. Path variable `departmentId`
2. Query object `PageQuery` (same binding style as `EmployeeListQuery`)
3. Call service
4. Return `PageResponse<EmployeeResponse>`

**Checkpoint**

- URL is under `/api/departments/...`, not a confusing `/api/employees/by-department/{id}` unless you consciously choose that.
- No business logic in the controller.

---

#### Step 6 — Manual verification with seed data

**Do**

1. Start the app.
2. Call a known department:

```http
GET /api/departments/2/employees?pageNumber=1&pageSize=5
```

3. Call a missing department:

```http
GET /api/departments/9999/employees
```

4. Optional: compare with the old filter:

```http
GET /api/employees/list?departmentId=2&pageNumber=1&pageSize=5
```

**Expected**

| Call | Result |
|------|--------|
| Existing department with employees | `200`, `content` size ≤ pageSize, `totalElements` matches MySQL count |
| Existing department with zero employees | `200`, empty `content`, `totalElements = 0` |
| Unknown department id | `404`, `statusCode = DEPARTMENT_NOT_FOUND` |

**Checkpoint**

- SQL log shows `WHERE` on `department_id` (filter in DB, not load-all-then-filter-in-Java).
- Response employees all belong to that department (spot-check `department.id` / name in DTO).

---

#### Step 7 — Reflect (write 3–5 lines)

Answer in this file or in Notion:

1. How is this different from `GET /api/employees/list?departmentId=`?
2. Why return `404` for a missing department instead of an empty page?
3. Why is `Page` better than `List` when Engineering has ~200 employees?
4. If a client sends department **name** instead of id, what breaks, and what would you need to change?

---

### Out of scope for this feature

Do not pull these in unless a real requirement appears:

- Filter by department **name** string
- Nested create: `POST /api/departments/{id}/employees`
- Department CRUD APIs
- Authorization (“only HR can see Finance”)
- Soft-delete / only `ACTIVE` employees (add later as a query param if needed)

---

### Implementation checklist

Use this while coding:

- [ ] Step 0: MySQL counts understood
- [ ] Step 1: API contract written
- [ ] Step 2: `DEPARTMENT_NOT_FOUND` (or equivalent) added
- [ ] Step 3: Repository method added
- [ ] Step 4: Service validates department then pages employees
- [ ] Step 5: `DepartmentController` endpoint added
- [ ] Step 6: Happy path + 404 + paging verified
- [ ] Step 7: Reflection notes written

---

### Suggested learning sequence after this feature

Only when this one feels solid:

1. Get employees by designation (`/api/designations/{id}/employees`) — same pattern, builds muscle memory  
2. Get employees by location  
3. Department summary: `GET /api/departments/{id}` with employee count (aggregation)

Do not jump to Specification / Criteria API yet. Your current JPQL + derived queries are enough for this problem.

---

## Feature: Employee manager (self-referencing relationship)

### Goal

Model and expose “this employee reports to that employee” using a foreign key on the same table, so create/read APIs understand managers and direct reports without treating manager as a free-text name.

### Why this feature is interesting

Department is `Employee → Department` (two different entities).  
Manager is `Employee → Employee` (same entity, different rows).

```text
Employee (report)
   │
   └── manager_id  ──►  Employee (manager)
```

In SQL this is still a normal foreign key: `employees.manager_id → employees.id`.  
In JPA it is still `@ManyToOne` — the target type just happens to be `Employee` again. You do **not** need a new annotation family for “self-reference.”

### What you already have (important discovery)

| Piece | Status today |
|-------|----------------|
| MySQL column `employees.manager_id` | Exists (nullable FK to `employees.id`) |
| Seed data | Many seeded rows already have a `manager_id` |
| `Employee` field | `@ManyToOne @JoinColumn(name = "manager_id") private Employee managerId;` — works, but **badly named** |
| `CreateEmployeeRequest.managerId` | Exists |
| `EmployeeService.createEmployee` | Already loads manager by id and calls `setManagerId(...)` |
| `EmployeeResponse` | Does **not** expose manager yet |
| `EmployeeMapper` | Does **not** map manager yet |
| Direct-reports API | Missing |
| Business rules (no self-manager, etc.) | Missing |

So this feature is mostly: **clean the model, finish the API surface, and add rules** — not invent the FK from scratch.

### Problem to articulate first

Answer these before writing code:

1. Is `manager` required or optional? (CEO / top of tree usually has `manager_id = NULL`.)
2. Can an employee be their own manager? (No — reject.)
3. Should create/update reject a missing manager id with `404`, or a dedicated `MANAGER_NOT_FOUND`?
4. What should JSON return — full nested `EmployeeResponse` for manager (risk of recursion), or a small summary (`id`, `firstName`, `lastName`, `email`)?
5. Do you need `GET /api/employees/{id}/reports` (direct reports) in this slice, or only “assign manager + show manager on employee response”?

Write your answers down before Step 1.

---

### Recommended approach for this project

**Slice A (must do first)** — make the existing relationship correct and visible:

1. Rename entity field `managerId` → `manager` (type stays `Employee`).
2. Map manager into a **small** response DTO (not a full recursive `EmployeeResponse`).
3. Keep create with `managerId` on the request; validate manager exists; reject self-assignment when updating later.

**Slice B (add when Slice A works)** — query the “many” side from the manager:

```http
GET /api/employees/{managerId}/reports?pageNumber=1&pageSize=10
```

Same idea as department employees: filter where `e.manager.id = :managerId`, page the result. No `@OneToMany List<Employee> reports` required on `Employee`.

Defer cycle detection (A manages B, B manages A) until you feel Slice A/B are solid — that is a real rule, but a second problem.

---

### Step-by-step plan

#### Step 0 — Confirm the database (read only)

**Do**

```sql
DESCRIBE employees;
-- confirm manager_id exists, NULL allowed

SELECT id, first_name, last_name, manager_id
FROM employees
ORDER BY id
LIMIT 15;

SELECT manager_id, COUNT(*) AS reports
FROM employees
WHERE manager_id IS NOT NULL
GROUP BY manager_id
ORDER BY reports DESC
LIMIT 10;

SELECT COUNT(*) FROM employees WHERE manager_id IS NULL;
```

**Checkpoint**

- You can point at one concrete row and say “employee X reports to employee Y.”
- You know some employees have no manager (top of tree / seed edge cases).

---

#### Step 1 — Fix the Java naming (entity)

**Do**

In `Employee.java`, rename:

```java
// bad: field is an Employee, not a Long
private Employee managerId;

// good: matches the domain word and @ManyToOne target
@ManyToOne
@JoinColumn(name = "manager_id")
private Employee manager;
```

Update every `getManagerId` / `setManagerId` call site in services (create path already uses them).

**Why**

`managerId` sounds like `Long`. The JPA association is the whole `Employee` manager object; the column name stays `manager_id` via `@JoinColumn`. Confusing names cause bugs when you later add a real `Long` id in DTOs.

**Checkpoint**

- Project compiles.
- MySQL column name unchanged (`manager_id`).
- You can explain: field name ≠ column name ≠ JSON request field name.

---

#### Step 2 — Decide the response shape (no infinite JSON)

**Do**

Add a small type, e.g. `ManagerSummaryResponse` or reuse a tiny nested object:

| Field | Why |
|-------|-----|
| `id` | Client can link |
| `firstName`, `lastName` | Human readable |
| `email` | Optional but useful |

**Do not** put full `EmployeeResponse` inside `EmployeeResponse` for manager without care — Jackson can recurse (`employee → manager → manager → …`) or pull lazy associations.

Wire `EmployeeMapper` to set `manager` only when `employee.getManager() != null`.

**Checkpoint**

- `GET` / create response shows manager summary or `null`.
- JSON for a deep hierarchy does not explode into megabytes.

---

#### Step 3 — Harden create (and later update) rules

**Do** in `EmployeeService.createEmployee` (you already load the manager):

1. If `managerId == null` → leave `manager` null (allowed).
2. If `managerId` present → `findById` or `404` / `MANAGER_NOT_FOUND`.
3. Prefer a dedicated `StatusCode` for missing manager (same lesson as department).

For **update** (when you touch it):

4. Reject `managerId == employeeId` (cannot manage yourself) with `400` / validation-style business error.
5. Optional later: reject if manager is `TERMINATED`.

**Checkpoint**

- Create with valid `managerId` persists `manager_id` in MySQL.
- Create with unknown `managerId` returns clear error, not a DB FK failure as 500.
- (Update) Self-manager is rejected before `save`.

---

#### Step 4 — Repository for direct reports (Slice B)

**Do**

```java
// EmployeeRepository — derived query is enough
Page<Employee> findByManagerId(Long managerId, Pageable pageable);
```

Or JPQL:

```java
@Query("""
        SELECT e FROM Employee e
        WHERE e.manager.id = :managerId
        """)
Page<Employee> findDirectReports(@Param("managerId") Long managerId, Pageable pageable);
```

Same lesson as department: query the many side; no `@OneToMany` required.

**Checkpoint**

- Method returns only employees whose `manager_id` equals the path id.
- Return type is `Page`, not `List`.

---

#### Step 5 — Service + controller for reports

**Do**

```http
GET /api/employees/{id}/reports?pageNumber=1&pageSize=10
```

Service flow:

1. Ensure employee `id` exists → else `404 EMPLOYEE_NOT_FOUND`.
2. `findByManagerId(id, pageable)`.
3. Map with `EmployeeMapper` → `PageResponse<EmployeeResponse>`.

Empty page means “this person exists but has no direct reports” — different from missing employee.

**Checkpoint**

- Known manager with reports → `200` + non-empty page.
- Known employee with zero reports → `200` + empty `content`.
- Unknown id → `404`.

---

#### Step 6 — Manual verification

**Do**

1. Pick a manager id from Step 0 SQL (someone with `reports > 0`).
2. Call `GET /api/employees/{id}/reports?pageNumber=1&pageSize=5`.
3. Create an employee with that `managerId`; confirm response includes manager summary and MySQL `manager_id` matches.
4. Try create with `managerId: 999999` → expect not-found error.
5. (If update ready) set manager to self → expect business error.

**Checkpoint**

- SQL log shows `WHERE manager_id = ?` for reports.
- Create INSERT includes `manager_id`.

---

#### Step 7 — Reflect

Write 3–5 lines:

1. How is manager different from department if both use `@ManyToOne`?
2. Why rename `managerId` → `manager` on the entity but keep `managerId` on the request DTO?
3. Why is a manager **summary** safer than nesting full `EmployeeResponse`?
4. If you later add `@OneToMany(mappedBy = "manager") List<Employee> reports`, what bug appears if that list is serialized on every employee GET?

---

### Out of scope for this feature

Do not pull these in yet unless a real need appears:

- Full org-chart / recursive tree API
- Cycle detection beyond “not self”
- “Skip-level” or all descendants queries (recursive CTE / closure table)
- `@OneToMany` reports collection on `Employee`
- Changing manager when current manager is deleted (ON DELETE SET NULL vs restrict) — confirm DB behavior, don’t redesign FK yet

---

### Implementation checklist

- [ ] Step 0: MySQL manager_id and sample rows understood
- [ ] Step 1: Entity field renamed to `manager`; call sites updated
- [ ] Step 2: Manager summary on `EmployeeResponse` + mapper
- [ ] Step 3: Create/update validation (exists, not self)
- [ ] Step 4: `findByManagerId` (or JPQL) added
- [ ] Step 5: `GET /api/employees/{id}/reports` wired
- [ ] Step 6: Happy path + empty reports + 404 + create verified
- [ ] Step 7: Reflection notes written

---

### Suggested learning sequence after this feature

1. Notion note: self-referencing `@ManyToOne` (same table FK)  
2. Optional: assign/change manager endpoint `PATCH /api/employees/{id}/manager`  
3. Only then: cycle detection or org tree — new problem, new design  
