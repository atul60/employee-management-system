# Note length: bad vs good

Use the **good** column as the quality bar. The bad column is what this skill forbids.

## Topic: `@RequestParam` is not required on `EmployeeListQuery`

### Bad (too short)

`EmployeeListQuery` is a command object. Spring binds query params to setters. No `@RequestParam` needed.

Self-check: Why does it work?

### Good (detailed enough to revise later)

**Why it exists.** List APIs grow: `search`, `pageNumber`, `pageSize`, `sortBy`, `status`. Ten `@RequestParam` arguments drift out of sync with the DTO. One object is the query contract.

**What problem it solves in EMS.** `GET /api/employees/list?search=Sneha&pageNumber=1` hits `getAllEmployees(EmployeeListQuery pageQuery)` with no annotations. HR still gets paging, search, and filters from the query string.

**How Spring Boot solves it.** The parameter is a custom class, not a `String`/`int`. Spring MVC treats it as a command object (`@ModelAttribute` is implicit). It constructs `EmployeeListQuery`, then for each query name calls the matching setter — including fields inherited from `PageQuery` (`pageNumber`, `sortBy`).

**Where it is used.** Any GET list/filter API: admin tables, search UIs, mobile “load more” screens.

**Internal working.**

1. DispatcherServlet matches `@GetMapping("/list")`.
2. `ServletModelAttributeMethodProcessor` sees a non-simple type without `@RequestBody`.
3. No-arg construct + `setSearch("Sneha")` + `setPageNumber(1)`.
4. Controller method runs.

**Common mistakes.** Adding `@RequestBody` on the list method — query params are ignored. Naming the query `page` while the field is `pageNumber` — the setter never runs.

**Best practices.** One query DTO per list resource. `@RequestParam` for a single flag. `@RequestBody` only for JSON bodies (create/update).

**Connects to.** `@RequestBody` (body ≠ query string). `PageQuery` (inherited setters still bind).

**EMS example.** `EmployeeController.getAllEmployees(EmployeeListQuery)` and fields on `EmployeeListQuery` / `PageQuery`.

**Quick self-check**

1. If you add `@RequestBody` to the list method, what happens to `?search=Sneha`?
   - Spring reads the body, not the query string. The search param is ignored (or the call fails with no body).
2. Why does `sortBy=salary` bind even though `sortBy` is on `PageQuery`?
   - Binding uses the runtime object. Inherited setters are visible.
3. When would you still write `@RequestParam`?
   - One isolated value, or when the query name must differ from the Java field name.
