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
