-- Seed ~1000 employees into employee_db.
-- Safe to re-run: skips emails that already exist (emp{n}@company.test).
-- Requires existing departments (1-5), designations (1-7), locations (1-3).

SET SESSION cte_max_recursion_depth = 1100;

INSERT INTO employees (
    first_name,
    last_name,
    email,
    phone_number,
    salary,
    joining_date,
    status,
    department_id,
    designation_id,
    location_id,
    manager_id
)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT
    ELT(1 + MOD(n, 20),
        'Aarav', 'Vivaan', 'Aditya', 'Vihaan', 'Arjun',
        'Sai', 'Reyansh', 'Ayaan', 'Krishna', 'Ishaan',
        'Ananya', 'Diya', 'Aadhya', 'Ira', 'Myra',
        'Sara', 'Anika', 'Navya', 'Kiara', 'Aisha'
    ) AS first_name,
    ELT(1 + MOD(n, 15),
        'Sharma', 'Verma', 'Patel', 'Reddy', 'Nair',
        'Iyer', 'Khan', 'Gupta', 'Mehta', 'Joshi',
        'Das', 'Rao', 'Singh', 'Kulkarni', 'Banerjee'
    ) AS last_name,
    CONCAT('emp', n, '@company.test') AS email,
    CAST(7000000000 + n AS CHAR) AS phone_number,
    CAST(400000 + MOD(n, 50) * 25000 AS DECIMAL(12, 2)) AS salary,
    DATE_SUB(CURDATE(), INTERVAL (30 + MOD(n, 2500)) DAY) AS joining_date,
    ELT(1 + MOD(n, 12),
        'ACTIVE', 'ACTIVE', 'ACTIVE', 'ACTIVE', 'ACTIVE', 'ACTIVE',
        'ACTIVE', 'ACTIVE', 'ON_LEAVE', 'ON_LEAVE', 'INACTIVE', 'TERMINATED'
    ) AS status,
    CASE MOD(n, 5)
        WHEN 0 THEN 1
        WHEN 1 THEN 2
        WHEN 2 THEN 3
        WHEN 3 THEN 4
        ELSE 5
    END AS department_id,
    CASE MOD(n, 5)
        WHEN 0 THEN IF(MOD(n, 40) = 0, 1, 2)
        WHEN 1 THEN ELT(1 + MOD(n, 3), 2, 3, 4)
        WHEN 2 THEN ELT(1 + MOD(n, 2), 5, 6)
        WHEN 3 THEN 7
        ELSE ELT(1 + MOD(n, 2), 3, 4)
    END AS designation_id,
    ELT(1 + MOD(n, 3), 1, 2, 3) AS location_id,
    CASE MOD(n, 5)
        WHEN 0 THEN 1
        WHEN 1 THEN 2
        WHEN 2 THEN 3
        WHEN 3 THEN 1
        ELSE 5
    END AS manager_id
FROM seq
WHERE NOT EXISTS (
    SELECT 1 FROM employees e WHERE e.email = CONCAT('emp', seq.n, '@company.test')
);
