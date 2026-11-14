package com.ems.gui;

import com.ems.db.DatasetLoader;
import com.ems.db.EmployeeDAO;
import com.ems.model.Employee;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * MainFrame.java - the dashboard.
 * Shows all employees in a table with FILTERS (search, department, designation, salary range)
 * and buttons for Add / Update / Delete / Import dataset / Export.
 */
public class MainFrame extends JFrame {

    private static final String ALL_DEPT = "All Departments";
    private static final String ALL_DESIG = "All Designations";
    private static final Color BLUE = new Color(30, 60, 114);

    private JTable employeeTable;
    private DefaultTableModel tableModel;

    // filter controls
    private JTextField txtSearch, txtMinSalary, txtMaxSalary;
    private JComboBox<String> cmbDepartment, cmbDesignation;
    private JLabel lblStatus;

    private boolean updatingCombos = false; // stops filter from firing while combos are refilled

    public MainFrame() {
        UIManager.put("Button.disabledText", Color.BLACK);

        setTitle("Employee Management System - Dashboard");
        setSize(1020, 620);
        setMinimumSize(new Dimension(900, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        refreshFilterOptions();
        applyFilters();
    }

    // ============================================================
    //  BUILD THE SCREEN
    // ============================================================
    private void initComponents() {
        setLayout(new BorderLayout());

        // ----- title bar -----
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        titleBar.setBackground(BLUE);
        JLabel title = new JLabel("Employee Management Dashboard");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        titleBar.add(title);

        // ----- filter panel -----
        JPanel filterPanel = buildFilterPanel();

        JPanel north = new JPanel(new BorderLayout());
        north.add(titleBar, BorderLayout.NORTH);
        north.add(filterPanel, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        // ----- table -----
        String[] cols = {"ID", "Name", "Salary (₹)", "Department", "Designation"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                if (c == 0) return Integer.class;   // so ID sorts numerically
                if (c == 2) return Double.class;    // so Salary sorts numerically
                return String.class;
            }
        };

        employeeTable = new JTable(tableModel) {
            // zebra stripes
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(238, 243, 252));
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        };
        employeeTable.setFont(new Font("Arial", Font.PLAIN, 14));
        employeeTable.setRowHeight(26);
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeeTable.setAutoCreateRowSorter(true); // click a column header to sort
        employeeTable.getColumnModel().getColumn(0).setMaxWidth(90);

        // salary shown as 55,000.00
        DefaultTableCellRenderer salaryRenderer = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) {
                setText(v instanceof Number ? String.format("%,.2f", ((Number) v).doubleValue()) : "");
            }
        };
        salaryRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        employeeTable.getColumnModel().getColumn(2).setCellRenderer(salaryRenderer);

        JTableHeader header = employeeTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(JLabel.CENTER);
                setBackground(BLUE);
                setForeground(Color.WHITE);
                setFont(new Font("Arial", Font.BOLD, 15));
                setOpaque(true);
            }
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                // show a small arrow on the column that is sorted
                RowSorter<?> sorter = t.getRowSorter();
                String arrow = "";
                if (sorter != null && !sorter.getSortKeys().isEmpty()) {
                    RowSorter.SortKey k = sorter.getSortKeys().get(0);
                    if (k.getColumn() == t.convertColumnIndexToModel(c)) {
                        arrow = k.getSortOrder() == SortOrder.ASCENDING ? "  ▲" : k.getSortOrder() == SortOrder.DESCENDING ? "  ▼" : "";
                    }
                }
                setText(String.valueOf(v) + arrow);
                setBackground(BLUE);
                setForeground(Color.WHITE);
                return this;
            }
        });

        add(new JScrollPane(employeeTable), BorderLayout.CENTER);

        // ----- bottom: buttons + status -----
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton add = createButton("Add", new Color(40, 167, 69));
        JButton update = createButton("Update", new Color(0, 123, 255));
        JButton delete = createButton("Delete", new Color(220, 53, 69));
        JButton refresh = createButton("Refresh", new Color(108, 117, 125));
        JButton imp = createButton("Import Dataset", new Color(111, 66, 193));
        JButton exp = createButton("Export CSV", new Color(23, 162, 184));

        add.addActionListener(e -> openAddDialog());
        update.addActionListener(e -> openUpdateDialog());
        delete.addActionListener(e -> deleteSelectedEmployee());
        refresh.addActionListener(e -> { refreshFilterOptions(); applyFilters(); });
        imp.addActionListener(e -> importDataset());
        exp.addActionListener(e -> exportDataset());

        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(refresh);
        buttons.add(imp);
        buttons.add(exp);

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Arial", Font.BOLD, 13));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 15, 8, 15));

        JPanel south = new JPanel(new BorderLayout());
        south.add(buttons, BorderLayout.CENTER);
        south.add(lblStatus, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildFilterPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBackground(new Color(245, 247, 250));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 205, 215)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));

        txtSearch = new JTextField(12);
        txtSearch.setToolTipText("Type part of a name or an ID");
        cmbDepartment = new JComboBox<>();
        cmbDesignation = new JComboBox<>();
        cmbDepartment.setPreferredSize(new Dimension(170, 26));
        cmbDesignation.setPreferredSize(new Dimension(190, 26));
        txtMinSalary = new JTextField(6);
        txtMaxSalary = new JTextField(6);

        p.add(new JLabel("Search (Name / ID):"));
        p.add(txtSearch);
        p.add(new JLabel("Department:"));
        p.add(cmbDepartment);
        p.add(new JLabel("Designation:"));
        p.add(cmbDesignation);
        p.add(new JLabel("Salary from:"));
        p.add(txtMinSalary);
        p.add(new JLabel("to:"));
        p.add(txtMaxSalary);

        JButton clear = new JButton("Clear Filters");
        clear.addActionListener(e -> clearFilters());
        p.add(clear);

        // Filter live: every change re-runs the filter
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        };
        txtSearch.getDocument().addDocumentListener(dl);
        txtMinSalary.getDocument().addDocumentListener(dl);
        txtMaxSalary.getDocument().addDocumentListener(dl);
        cmbDepartment.addActionListener(e -> { if (!updatingCombos) applyFilters(); });
        cmbDesignation.addActionListener(e -> { if (!updatingCombos) applyFilters(); });

        return p;
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(135, 34));
        return btn;
    }

    // ============================================================
    //  FILTER LOGIC
    // ============================================================

    /** Refills the Department / Designation drop-downs from the database (keeps current selection if possible). */
    private void refreshFilterOptions() {
        updatingCombos = true;
        Object selDept = cmbDepartment.getSelectedItem();
        Object selDesig = cmbDesignation.getSelectedItem();

        cmbDepartment.removeAllItems();
        cmbDepartment.addItem(ALL_DEPT);
        for (String d : EmployeeDAO.getDistinctValues("department")) cmbDepartment.addItem(d);

        cmbDesignation.removeAllItems();
        cmbDesignation.addItem(ALL_DESIG);
        for (String d : EmployeeDAO.getDistinctValues("designation")) cmbDesignation.addItem(d);

        if (selDept != null) cmbDepartment.setSelectedItem(selDept);
        if (selDesig != null) cmbDesignation.setSelectedItem(selDesig);
        if (cmbDepartment.getSelectedIndex() < 0) cmbDepartment.setSelectedIndex(0);
        if (cmbDesignation.getSelectedIndex() < 0) cmbDesignation.setSelectedIndex(0);
        updatingCombos = false;
    }

    private void clearFilters() {
        updatingCombos = true;
        txtSearch.setText("");
        txtMinSalary.setText("");
        txtMaxSalary.setText("");
        cmbDepartment.setSelectedIndex(0);
        cmbDesignation.setSelectedIndex(0);
        updatingCombos = false;
        applyFilters();
    }

    /** Reads the filter controls, asks the DAO for matching employees and fills the table. */
    private void applyFilters() {
        if (updatingCombos || tableModel == null) return;

        Double min = parseSalary(txtMinSalary.getText());
        Double max = parseSalary(txtMaxSalary.getText());
        boolean badMin = !txtMinSalary.getText().trim().isEmpty() && min == null;
        boolean badMax = !txtMaxSalary.getText().trim().isEmpty() && max == null;
        if (badMin || badMax) {
            lblStatus.setForeground(new Color(200, 0, 0));
            lblStatus.setText("Salary range must be a number (e.g. 30000)");
            return;
        }

        String dept = (String) cmbDepartment.getSelectedItem();
        String desig = (String) cmbDesignation.getSelectedItem();
        if (ALL_DEPT.equals(dept)) dept = null;
        if (ALL_DESIG.equals(desig)) desig = null;

        List<Employee> list = EmployeeDAO.filterEmployees(txtSearch.getText(), dept, desig, min, max);
        fillTable(list);

        double total = 0;
        for (Employee e : list) total += e.getSalary();
        lblStatus.setForeground(new Color(30, 60, 114));
        lblStatus.setText(String.format("Showing %d of %d employees   |   Total salary of shown: ₹ %,.2f",
                list.size(), EmployeeDAO.countEmployees(), total));
    }

    private Double parseSalary(String s) {
        try {
            if (s == null || s.trim().isEmpty()) return null;
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void fillTable(List<Employee> list) {
        tableModel.setRowCount(0);
        for (Employee e : list) {
            tableModel.addRow(new Object[]{
                    e.getId(), e.getName(), e.getSalary(), e.getDepartment(), e.getDesignation()
            });
        }
    }

    // ============================================================
    //  CRUD ACTIONS
    // ============================================================
    private void openAddDialog() {
        new EmployeeFormDialog(this, "Add Employee", null).setVisible(true);
        refreshFilterOptions();
        applyFilters();
    }

    private void openUpdateDialog() {
        int id = getSelectedEmployeeId();
        if (id == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee in the table first.");
            return;
        }
        Employee emp = EmployeeDAO.getEmployeeById(id);
        new EmployeeFormDialog(this, "Update Employee", emp).setVisible(true);
        refreshFilterOptions();
        applyFilters();
    }

    private void deleteSelectedEmployee() {
        int id = getSelectedEmployeeId();
        if (id == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee in the table first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete employee " + id + "? This cannot be undone.", "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (EmployeeDAO.deleteEmployee(id)) {
                JOptionPane.showMessageDialog(this, "Deleted successfully");
                refreshFilterOptions();
                applyFilters();
            } else {
                JOptionPane.showMessageDialog(this, "Delete failed");
            }
        }
    }

    /** Returns the ID of the selected row (works even when the table is sorted), or -1. */
    private int getSelectedEmployeeId() {
        int viewRow = employeeTable.getSelectedRow();
        if (viewRow == -1) return -1;
        int modelRow = employeeTable.convertRowIndexToModel(viewRow);
        return (int) tableModel.getValueAt(modelRow, 0);
    }

    // ============================================================
    //  DATASET IMPORT / EXPORT
    // ============================================================
    private void importDataset() {
        JFileChooser fc = new JFileChooser(new File("data"));
        fc.setDialogTitle("Choose a dataset CSV file (id,name,salary,department,designation)");
        fc.setSelectedFile(new File(DatasetLoader.DEFAULT_DATASET));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        int added = DatasetLoader.importFromCsv(fc.getSelectedFile());
        refreshFilterOptions();
        applyFilters();
        JOptionPane.showMessageDialog(this,
                added + " new employees imported.\n(Rows whose ID already exists are skipped.)");
    }

    private void exportDataset() {
        JFileChooser fc = new JFileChooser(new File("."));
        fc.setSelectedFile(new File("employees_export.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        if (DatasetLoader.exportToCsv(fc.getSelectedFile())) {
            JOptionPane.showMessageDialog(this, "Exported to:\n" + fc.getSelectedFile().getAbsolutePath());
        } else {
            JOptionPane.showMessageDialog(this, "Export failed", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
