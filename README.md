# Employee Management System
**Java Swing + SQLite (JDBC)** — College project

## What's new
- **Dataset**: `data/employees.csv` holds 60 sample employees. On first run the app loads them into the database (`ems.db`) automatically.
- **Filters** on the dashboard (they work live as you type / select):
  - Search by name or ID
  - Department drop-down
  - Designation drop-down
  - Salary range (from – to)
  - **Clear Filters** button, and a status line showing "Showing X of Y employees"
- Click any column header to **sort** (ID and Salary sort numerically).
- **Import Dataset** button – load any CSV (`id,name,salary,department,designation`); duplicate IDs are skipped.
- **Export CSV** button – save the current database to a CSV file.
- Add / Update / Delete still work as before.

## How to run in VS Code
1. Install **JDK 17+** and the VS Code **Extension Pack for Java**.
2. `File → Open Folder` → select this `EmployeeManagementSystem` folder (the one that contains `src`, `lib`, `data`).
3. Open `src/com/ems/Main.java` and click **Run** (or press F5).
4. Login: `admin` / `admin123`

The SQLite jar is already in `lib/` (no download needed).

No VS Code? Run `run.bat` (Windows) or `./run.sh` (Linux/Mac).

## Project structure
```
EmployeeManagementSystem/
├── data/employees.csv          <- DATASET (60 employees)
├── ems.db                      <- SQLite database (already filled with the dataset)
├── lib/sqlite-jdbc-3.53.0.0.jar
├── sql/schema.sql
└── src/com/ems/
    ├── Main.java
    ├── model/Employee.java
    ├── db/DBConnection.java    <- connection
    ├── db/EmployeeDAO.java     <- CRUD + filterEmployees()
    ├── db/DatasetLoader.java   <- CSV import / export
    ├── gui/LoginFrame.java
    ├── gui/MainFrame.java      <- dashboard + filters
    ├── gui/EmployeeFormDialog.java
    └── utils/Validator.java
```

## Reset the data
Delete `ems.db` and run again — the dataset is reloaded automatically.

## Common problems
| Problem | Fix |
|---|---|
| `ClassNotFoundException: org.sqlite.JDBC` | Make sure the folder opened in VS Code is the one containing `lib/`; use the "Run EMS Application" launch config |
| Table is empty | Run from the project folder (so `data/employees.csv` is found), or click **Import Dataset** |
| Login fails | `admin` / `admin123` |
