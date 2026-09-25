-- Make employee_db internally consistent after the 1000-employee seed.
-- Safe to re-run: each insert skips rows that already exist.

-- ---------------------------------------------------------------------------
-- 1. Leave types (master data used by leave_requests)
-- ---------------------------------------------------------------------------
INSERT INTO leave_types (name, max_days_per_year)
SELECT v.name, v.max_days_per_year
FROM (
    SELECT 'Maternity' AS name, 180 AS max_days_per_year
    UNION ALL SELECT 'Paternity', 15
    UNION ALL SELECT 'Compensatory', 5
) v
WHERE NOT EXISTS (
    SELECT 1 FROM leave_types lt WHERE lt.name = v.name
);

-- ---------------------------------------------------------------------------
-- 2. Projects per department so assignments can stay department-local
-- ---------------------------------------------------------------------------
INSERT INTO projects (name, code, department_id, start_date, end_date, status)
SELECT v.name, v.code, v.department_id, v.start_date, v.end_date, v.status
FROM (
    SELECT 'Project Phoenix' AS name, 'PHX' AS code, 2 AS department_id,
           DATE('2025-03-01') AS start_date, CAST(NULL AS DATE) AS end_date, 'ACTIVE' AS status
    UNION ALL SELECT 'Project Nexus', 'NEX', 2, DATE('2024-11-01'), NULL, 'ACTIVE'
    UNION ALL SELECT 'Project Orion', 'ORN', 2, DATE('2023-06-01'), DATE('2025-06-30'), 'COMPLETED'
    UNION ALL SELECT 'Mobile App', 'MOB', 2, DATE('2025-07-01'), NULL, 'ACTIVE'
    UNION ALL SELECT 'Platform Mesh', 'MSH', 5, DATE('2025-02-01'), NULL, 'ACTIVE'
    UNION ALL SELECT 'API Gateway', 'GWY', 5, DATE('2024-09-01'), NULL, 'ACTIVE'
    UNION ALL SELECT 'Data Lake', 'DLK', 5, DATE('2025-06-01'), NULL, 'ACTIVE'
    UNION ALL SELECT 'Observability Stack', 'OBS', 5, DATE('2023-01-15'), DATE('2024-12-31'), 'COMPLETED'
    UNION ALL SELECT 'Ledger Core', 'LDG', 4, DATE('2025-04-01'), NULL, 'ACTIVE'
    UNION ALL SELECT 'Tax Engine', 'TAX', 4, DATE('2024-01-10'), DATE('2025-09-30'), 'COMPLETED'
    UNION ALL SELECT 'Onboarding Portal', 'ONB', 3, DATE('2025-05-01'), NULL, 'ACTIVE'
    UNION ALL SELECT 'Policy Hub', 'POL', 3, DATE('2024-03-01'), NULL, 'ACTIVE'
    UNION ALL SELECT 'Board Reporting', 'BRD', 1, DATE('2025-01-01'), NULL, 'ACTIVE'
    UNION ALL SELECT 'Compliance Desk', 'CMP', 1, DATE('2024-06-01'), NULL, 'ACTIVE'
    UNION ALL SELECT 'Org Redesign', 'ORG', 1, DATE('2023-04-01'), DATE('2024-11-30'), 'COMPLETED'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM projects p WHERE p.code = v.code
);

-- ---------------------------------------------------------------------------
-- 3. CURRENT address for every employee, city aligned with work location
-- ---------------------------------------------------------------------------
INSERT INTO employee_addresses (employee_id, type, line1, city, state, pincode)
SELECT
    e.id,
    'CURRENT',
    CONCAT(
        10 + MOD(e.id, 89),
        ' ',
        ELT(1 + MOD(e.id, 8),
            'MG Road', 'Residency Road', 'Lake View', 'FC Road',
            'Koramangala 5th Block', 'Baner Road', 'Park Street', 'Linking Road')
    ),
    CASE e.location_id
        WHEN 1 THEN 'Bengaluru'
        WHEN 2 THEN 'Pune'
        ELSE ELT(1 + MOD(e.id, 6), 'Hyderabad', 'Chennai', 'Delhi', 'Kolkata', 'Jaipur', 'Ahmedabad')
    END,
    CASE e.location_id
        WHEN 1 THEN 'Karnataka'
        WHEN 2 THEN 'Maharashtra'
        ELSE ELT(1 + MOD(e.id, 6), 'Telangana', 'Tamil Nadu', 'Delhi', 'West Bengal', 'Rajasthan', 'Gujarat')
    END,
    CASE e.location_id
        WHEN 1 THEN LPAD(560001 + MOD(e.id, 90), 6, '0')
        WHEN 2 THEN LPAD(411001 + MOD(e.id, 90), 6, '0')
        ELSE LPAD(
            ELT(1 + MOD(e.id, 6), 500001, 600001, 110001, 700001, 302001, 380001) + MOD(e.id, 80),
            6, '0'
        )
    END
FROM employees e
WHERE NOT EXISTS (
    SELECT 1
    FROM employee_addresses a
    WHERE a.employee_id = e.id AND a.type = 'CURRENT'
);

-- ---------------------------------------------------------------------------
-- 4. PERMANENT (hometown) address for most employees
-- ---------------------------------------------------------------------------
INSERT INTO employee_addresses (employee_id, type, line1, city, state, pincode)
SELECT
    e.id,
    'PERMANENT',
    CONCAT(
        20 + MOD(e.id, 70),
        ' ',
        ELT(1 + MOD(e.id, 6),
            'Temple Street', 'Station Road', 'Gandhi Nagar',
            'Civil Lines', 'Old City', 'Market Road')
    ),
    ELT(1 + MOD(e.id, 8),
        'Patna', 'Lucknow', 'Nagpur', 'Indore',
        'Coimbatore', 'Mysuru', 'Vadodara', 'Bhopal'),
    ELT(1 + MOD(e.id, 8),
        'Bihar', 'Uttar Pradesh', 'Maharashtra', 'Madhya Pradesh',
        'Tamil Nadu', 'Karnataka', 'Gujarat', 'Madhya Pradesh'),
    LPAD(
        ELT(1 + MOD(e.id, 8),
            800001, 226001, 440001, 452001,
            641001, 570001, 390001, 462001) + MOD(e.id, 50),
        6, '0'
    )
FROM employees e
WHERE MOD(e.id, 7) <> 0
  AND NOT EXISTS (
      SELECT 1
      FROM employee_addresses a
      WHERE a.employee_id = e.id AND a.type = 'PERMANENT'
  );

-- ---------------------------------------------------------------------------
-- 5. Current approved leave for everyone marked ON_LEAVE
-- ---------------------------------------------------------------------------
INSERT INTO leave_requests (employee_id, leave_type_id, start_date, end_date, status, reviewed_by)
SELECT
    e.id,
    1 + MOD(e.id, 3),
    DATE_SUB(CURDATE(), INTERVAL (1 + MOD(e.id, 4)) DAY),
    DATE_ADD(CURDATE(), INTERVAL (2 + MOD(e.id, 6)) DAY),
    'APPROVED',
    e.manager_id
FROM employees e
WHERE e.status = 'ON_LEAVE'
  AND NOT EXISTS (
      SELECT 1
      FROM leave_requests lr
      WHERE lr.employee_id = e.id
        AND lr.status = 'APPROVED'
        AND lr.start_date <= CURDATE()
        AND lr.end_date >= CURDATE()
  );

-- ---------------------------------------------------------------------------
-- 6. Historical / pending / rejected leaves for a large employee subset
-- ---------------------------------------------------------------------------
INSERT INTO leave_requests (employee_id, leave_type_id, start_date, end_date, status, reviewed_by)
SELECT
    e.id,
    1 + MOD(e.id + n.n, 3),
    DATE_SUB(CURDATE(), INTERVAL (80 + MOD(e.id * n.n, 400)) DAY) AS start_date,
    DATE_ADD(
        DATE_SUB(CURDATE(), INTERVAL (80 + MOD(e.id * n.n, 400)) DAY),
        INTERVAL (1 + MOD(e.id + n.n, 4)) DAY
    ) AS end_date,
    ELT(1 + MOD(e.id + n.n, 5), 'APPROVED', 'APPROVED', 'APPROVED', 'REJECTED', 'PENDING'),
    CASE
        WHEN ELT(1 + MOD(e.id + n.n, 5), 'APPROVED', 'APPROVED', 'APPROVED', 'REJECTED', 'PENDING') = 'PENDING'
            THEN NULL
        ELSE e.manager_id
    END
FROM employees e
JOIN (
    SELECT 1 AS n
    UNION ALL SELECT 2
) n
WHERE e.status <> 'TERMINATED'
  AND DATE_SUB(CURDATE(), INTERVAL (80 + MOD(e.id * n.n, 400)) DAY) >= e.joining_date
  AND NOT (
      e.status IN ('INACTIVE', 'ON_LEAVE')
      AND ELT(1 + MOD(e.id + n.n, 5), 'APPROVED', 'APPROVED', 'APPROVED', 'REJECTED', 'PENDING') = 'PENDING'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM leave_requests lr
      WHERE lr.employee_id = e.id
        AND lr.start_date = DATE_SUB(CURDATE(), INTERVAL (80 + MOD(e.id * n.n, 400)) DAY)
  );

-- TERMINATED employees only get old approved/rejected leaves, never pending.
INSERT INTO leave_requests (employee_id, leave_type_id, start_date, end_date, status, reviewed_by)
SELECT
    e.id,
    1 + MOD(e.id, 3),
    DATE_SUB(e.joining_date, INTERVAL 0 DAY) + INTERVAL 40 DAY,
    DATE_SUB(e.joining_date, INTERVAL 0 DAY) + INTERVAL 42 DAY,
    ELT(1 + MOD(e.id, 2), 'APPROVED', 'REJECTED'),
    e.manager_id
FROM employees e
WHERE e.status = 'TERMINATED'
  AND e.joining_date <= DATE_SUB(CURDATE(), INTERVAL 45 DAY)
  AND NOT EXISTS (
      SELECT 1
      FROM leave_requests lr
      WHERE lr.employee_id = e.id
        AND lr.start_date = DATE_ADD(e.joining_date, INTERVAL 40 DAY)
  );

-- ---------------------------------------------------------------------------
-- 7. Active project assignments (same department as the employee)
-- ---------------------------------------------------------------------------
INSERT INTO project_assignments (employee_id, project_id, role, allocation_percent, start_date, end_date)
SELECT
    e.id,
    ap.id,
    'MEMBER',
    ELT(1 + MOD(e.id, 4), 25, 50, 75, 100),
    GREATEST(e.joining_date, ap.start_date),
    NULL
FROM employees e
JOIN (
    SELECT
        id,
        department_id,
        start_date,
        ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY id) AS rn,
        COUNT(*) OVER (PARTITION BY department_id) AS cnt
    FROM projects
    WHERE status = 'ACTIVE'
) ap
  ON ap.department_id = e.department_id
 AND ap.rn = 1 + MOD(e.id, ap.cnt)
WHERE e.status IN ('ACTIVE', 'ON_LEAVE')
  AND NOT EXISTS (
      SELECT 1
      FROM project_assignments pa
      WHERE pa.employee_id = e.id AND pa.project_id = ap.id
  );

-- Second active assignment for ~1/3 of working employees (different project).
INSERT INTO project_assignments (employee_id, project_id, role, allocation_percent, start_date, end_date)
SELECT
    e.id,
    ap.id,
    'MEMBER',
    ELT(1 + MOD(e.id, 3), 25, 25, 50),
    GREATEST(e.joining_date, ap.start_date),
    NULL
FROM employees e
JOIN (
    SELECT
        id,
        department_id,
        start_date,
        ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY id) AS rn,
        COUNT(*) OVER (PARTITION BY department_id) AS cnt
    FROM projects
    WHERE status = 'ACTIVE'
) ap
  ON ap.department_id = e.department_id
 AND ap.cnt > 1
 AND ap.rn = 1 + MOD(e.id + 1, ap.cnt)
WHERE e.status IN ('ACTIVE', 'ON_LEAVE')
  AND MOD(e.id, 3) = 0
  AND NOT EXISTS (
      SELECT 1
      FROM project_assignments pa
      WHERE pa.employee_id = e.id AND pa.project_id = ap.id
  );

-- One LEAD per active project if the project does not already have one.
UPDATE project_assignments pa
JOIN (
    SELECT x.project_id, MIN(x.employee_id) AS lead_employee_id
    FROM project_assignments x
    JOIN projects p ON p.id = x.project_id AND p.status = 'ACTIVE'
    JOIN employees e ON e.id = x.employee_id
    WHERE e.status IN ('ACTIVE', 'ON_LEAVE')
      AND NOT EXISTS (
          SELECT 1
          FROM project_assignments existing
          WHERE existing.project_id = x.project_id
            AND existing.role = 'LEAD'
      )
    GROUP BY x.project_id
) picked ON picked.project_id = pa.project_id
       AND picked.lead_employee_id = pa.employee_id
SET pa.role = 'LEAD';

-- Original demo data put a Platform employee on an Engineering project.
DELETE pa
FROM project_assignments pa
JOIN employees e ON e.id = pa.employee_id
JOIN projects p ON p.id = pa.project_id
WHERE e.department_id <> p.department_id;

-- ---------------------------------------------------------------------------
-- 8. Historical assignments on COMPLETED projects
-- ---------------------------------------------------------------------------
INSERT INTO project_assignments (employee_id, project_id, role, allocation_percent, start_date, end_date)
SELECT
    e.id,
    cp.id,
    'MEMBER',
    ELT(1 + MOD(e.id, 3), 50, 75, 100),
    GREATEST(e.joining_date, cp.start_date),
    cp.end_date
FROM employees e
JOIN (
    SELECT
        id,
        department_id,
        start_date,
        end_date,
        ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY id) AS rn,
        COUNT(*) OVER (PARTITION BY department_id) AS cnt
    FROM projects
    WHERE status = 'COMPLETED'
) cp
  ON cp.department_id = e.department_id
 AND cp.rn = 1 + MOD(e.id, cp.cnt)
WHERE MOD(e.id, 4) = 0
  AND e.joining_date <= cp.end_date
  AND NOT EXISTS (
      SELECT 1
      FROM project_assignments pa
      WHERE pa.employee_id = e.id AND pa.project_id = cp.id
  );
