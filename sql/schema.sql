-- Employee Management System - Database Schema (SQLite, file: ems.db)
-- The app creates this table automatically and loads data/employees.csv on first run.

CREATE TABLE IF NOT EXISTS employees (
    id          INTEGER PRIMARY KEY,   -- Unique Employee ID
    name        TEXT    NOT NULL,
    salary      REAL    NOT NULL,
    department  TEXT    NOT NULL,
    designation TEXT    NOT NULL
);

-- Useful queries (open ems.db in "DB Browser for SQLite")
SELECT * FROM employees;
SELECT * FROM employees WHERE department = 'Finance';
SELECT * FROM employees WHERE salary BETWEEN 40000 AND 70000;
SELECT department, COUNT(*) AS total, ROUND(AVG(salary),2) AS avg_salary
FROM employees GROUP BY department;
