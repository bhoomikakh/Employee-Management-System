package com.ems.db;

import com.ems.model.Employee;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * EmployeeDAO.java (DAO = Data Access Object)
 * All database operations for Employee live here:
 *   - Create table
 *   - Add, View, Search, Update, Delete
 *   - Filter (by keyword, department, designation, salary range)
 */
public class EmployeeDAO {

    // Converts the current row of a ResultSet into an Employee object
    private static Employee mapRow(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("salary"),
                rs.getString("department"),
                rs.getString("designation"));
    }

    /** Creates the employees table if it doesn't already exist. */
    public static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS employees ("
                + "id INTEGER PRIMARY KEY, "
                + "name TEXT NOT NULL, "
                + "salary REAL NOT NULL, "
                + "department TEXT NOT NULL, "
                + "designation TEXT NOT NULL"
                + ");";
        try (Statement stmt = DBConnection.getConnection().createStatement()) {
            stmt.execute(sql);
            System.out.println("Employee table ready.");
        } catch (SQLException e) {
            System.out.println("Failed to create table.");
            e.printStackTrace();
        }
    }

    /** Adds a new employee. Returns false if the ID already exists or on error. */
    public static boolean addEmployee(Employee emp) {
        if (getEmployeeById(emp.getId()) != null) {
            return false; // duplicate ID
        }
        String sql = "INSERT INTO employees (id, name, salary, department, designation) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, emp.getId());
            ps.setString(2, emp.getName());
            ps.setDouble(3, emp.getSalary());
            ps.setString(4, emp.getDepartment());
            ps.setString(5, emp.getDesignation());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Returns every employee, ordered by ID. */
    public static List<Employee> getAllEmployees() {
        return filterEmployees(null, null, null, null, null);
    }

    /**
     * FILTER: returns employees matching all the given conditions.
     * Any parameter that is null/empty is ignored.
     *
     * @param keyword     matches part of the Name OR part of the ID
     * @param department  exact department
     * @param designation exact designation
     * @param minSalary   salary >= minSalary
     * @param maxSalary   salary <= maxSalary
     */
    public static List<Employee> filterEmployees(String keyword, String department, String designation,
                                                 Double minSalary, Double maxSalary) {
        List<Employee> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM employees WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(name) LIKE ? OR CAST(id AS TEXT) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }
        if (department != null && !department.isEmpty()) {
            sql.append(" AND department = ?");
            params.add(department);
        }
        if (designation != null && !designation.isEmpty()) {
            sql.append(" AND designation = ?");
            params.add(designation);
        }
        if (minSalary != null) {
            sql.append(" AND salary >= ?");
            params.add(minSalary);
        }
        if (maxSalary != null) {
            sql.append(" AND salary <= ?");
            params.add(maxSalary);
        }
        sql.append(" ORDER BY id");

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Double) ps.setDouble(i + 1, (Double) p);
                else ps.setString(i + 1, (String) p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Finds one employee by ID, or null if not found. */
    public static Employee getEmployeeById(int id) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Updates an existing employee (matched by ID). */
    public static boolean updateEmployee(Employee emp) {
        String sql = "UPDATE employees SET name=?, salary=?, department=?, designation=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, emp.getName());
            ps.setDouble(2, emp.getSalary());
            ps.setString(3, emp.getDepartment());
            ps.setString(4, emp.getDesignation());
            ps.setInt(5, emp.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Deletes an employee by ID. */
    public static boolean deleteEmployee(int id) {
        String sql = "DELETE FROM employees WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Total number of employees in the table. */
    public static int countEmployees() {
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM employees")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Distinct values of a column (used to fill the filter drop-downs). Only 'department' or 'designation' allowed. */
    public static List<String> getDistinctValues(String column) {
        List<String> values = new ArrayList<>();
        if (!column.equals("department") && !column.equals("designation")) return values;
        String sql = "SELECT DISTINCT " + column + " FROM employees ORDER BY " + column;
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) values.add(rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return values;
    }
}
