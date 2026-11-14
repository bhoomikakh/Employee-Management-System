package com.ems.db;

import com.ems.model.Employee;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * DatasetLoader.java
 * Loads the employee DATASET (data/employees.csv) into the database,
 * and can also export the current database back to a CSV file.
 *
 * CSV format (first line is the header):
 *   id,name,salary,department,designation
 */
public class DatasetLoader {

    /** Default dataset file, relative to the project folder. */
    public static final String DEFAULT_DATASET = "data" + File.separator + "employees.csv";

    /** On first run (empty table) automatically loads the default dataset. */
    public static void seedIfEmpty() {
        if (EmployeeDAO.countEmployees() > 0) return;
        File f = new File(DEFAULT_DATASET);
        if (f.exists()) {
            int n = importFromCsv(f);
            System.out.println("Dataset loaded: " + n + " employees imported from " + f.getPath());
        } else {
            System.out.println("Dataset file not found: " + f.getAbsolutePath()
                    + "  (run the app from the project folder)");
        }
    }

    /**
     * Imports employees from a CSV file. Rows with an ID that already exists,
     * or rows that are invalid, are skipped.
     * @return number of employees actually inserted
     */
    public static int importFromCsv(File file) {
        int inserted = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        ) {
            Connection conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // one transaction = much faster for many rows
            String sql = "INSERT OR IGNORE INTO employees (id, name, salary, department, designation) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String line;
                boolean first = true;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (first) { // skip header row
                        first = false;
                        if (line.toLowerCase().startsWith("id")) continue;
                    }
                    String[] p = line.split(",");
                    if (p.length < 5) continue;
                    try {
                        ps.setInt(1, Integer.parseInt(p[0].trim()));
                        ps.setString(2, p[1].trim());
                        ps.setDouble(3, Double.parseDouble(p[2].trim()));
                        ps.setString(4, p[3].trim());
                        ps.setString(5, p[4].trim());
                        inserted += ps.executeUpdate();
                    } catch (NumberFormatException ex) {
                        // bad row -> skip it
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
        return inserted;
    }

    /** Saves every employee in the database to a CSV file. */
    public static boolean exportToCsv(File file) {
        List<Employee> all = EmployeeDAO.getAllEmployees();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            pw.println("id,name,salary,department,designation");
            for (Employee e : all) {
                pw.println(e.getId() + "," + e.getName() + "," + e.getSalary() + ","
                        + e.getDepartment() + "," + e.getDesignation());
            }
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
