package com.example.recordmaintenance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteConstraintException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class EmployeeRepository {

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private EmployeeCodeGenerator codeGenerator;

    // Static map to store plain passwords for admin viewing (demo purposes)
    private static final Map<String, String> passwordMap = new HashMap<>();

    public EmployeeRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
        codeGenerator = new EmployeeCodeGenerator(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
        // Migrate existing employees on first open
        migrateExistingEmployeePasswords();
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    /**
     * Insert Employee with auto-generated code and password
     */
    public InsertResult insertEmployee(Employee employee) {
        database.beginTransaction();
        try {
            // Generate unique employee code
            String empCode = codeGenerator.generateEmployeeCode();
            employee.setEmpId(empCode);

            // Generate initial password
            String initialPassword = EmployeeCodeGenerator.generateInitialPassword(
                    employee.getEmpName(), empCode);
            String hashedPassword = EmployeeCodeGenerator.hashPassword(initialPassword);
            employee.setEmpPassword(hashedPassword);

            // Store plain password for admin viewing
            passwordMap.put(empCode, initialPassword);

            // Insert Master
            ContentValues masterValues = new ContentValues();
            masterValues.put(DatabaseHelper.EMP_ID, employee.getEmpId());
            masterValues.put(DatabaseHelper.EMP_NAME, employee.getEmpName());
            masterValues.put(DatabaseHelper.EMP_EMAIL, employee.getEmpEmail());
            masterValues.put(DatabaseHelper.EMP_PASSWORD, employee.getEmpPassword());
            masterValues.put(DatabaseHelper.DESIGNATION, employee.getDesignation());
            masterValues.put(DatabaseHelper.DEPARTMENT, employee.getDepartment());
            masterValues.put(DatabaseHelper.JOINED_DATE, employee.getJoinedDate());
            masterValues.put(DatabaseHelper.SALARY, employee.getSalary());
            masterValues.put(DatabaseHelper.PASSWORD_CHANGED, employee.isPasswordChanged() ? 1 : 0);

            long mastCode = database.insert(DatabaseHelper.TABLE_MASTER, null, masterValues);

            if (mastCode != -1) {
                // Insert Detail
                ContentValues detailValues = new ContentValues();
                detailValues.put(DatabaseHelper.EMP_CODE, mastCode);
                detailValues.put(DatabaseHelper.ADDRESS_LINE1, employee.getAddressLine1());
                detailValues.put(DatabaseHelper.ADDRESS_LINE2, employee.getAddressLine2());
                detailValues.put(DatabaseHelper.CITY, employee.getCity());
                detailValues.put(DatabaseHelper.STATE, employee.getState());
                detailValues.put(DatabaseHelper.COUNTRY, employee.getCountry());

                long detailResult = database.insert(DatabaseHelper.TABLE_DETAIL, null, detailValues);

                if (detailResult != -1) {
                    database.setTransactionSuccessful();
                    return new InsertResult(true, mastCode, empCode, initialPassword, "Employee added successfully");
                }
            }
            return new InsertResult(false, -1, "", "", "Failed to insert employee");
        } catch (SQLiteConstraintException e) {
            if (e.getMessage().contains("EmpEmail")) {
                return new InsertResult(false, -1, "", "", "Email address already exists");
            } else if (e.getMessage().contains("EmpID")) {
                return new InsertResult(false, -1, "", "", "Employee ID already exists");
            } else {
                return new InsertResult(false, -1, "", "", "Constraint violation: " + e.getMessage());
            }
        } catch (Exception e) {
            return new InsertResult(false, -1, "", "", "Error: " + e.getMessage());
        } finally {
            database.endTransaction();
        }
    }

    /**
     * Get employee password for admin viewing (returns plain text password)
     */
    public String getEmployeePassword(String empId) {
        // Check if we have the plain password stored
        if (passwordMap.containsKey(empId)) {
            return passwordMap.get(empId);
        }

        // If not found, try to generate it for existing employees
        Employee emp = getEmployeeByEmpId(empId);
        if (emp != null) {
            String generatedPassword = EmployeeCodeGenerator.generateInitialPassword(emp.getEmpName(), empId);
            passwordMap.put(empId, generatedPassword);

            // Update database with new hashed password if current is empty
            if (emp.getEmpPassword() == null || emp.getEmpPassword().trim().isEmpty()) {
                String hashedPassword = EmployeeCodeGenerator.hashPassword(generatedPassword);
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.EMP_PASSWORD, hashedPassword);
                values.put(DatabaseHelper.PASSWORD_CHANGED, 0);
                database.update(DatabaseHelper.TABLE_MASTER, values,
                        DatabaseHelper.EMP_ID + " = ?", new String[]{empId});
            }

            return generatedPassword;
        }

        return "Password not available";
    }

    /**
     * Get employee by Employee ID with full details
     */
    public Employee getEmployeeByEmpId(String empId) {
        String query = "SELECT m.*, d.* FROM " + DatabaseHelper.TABLE_MASTER + " m " +
                "LEFT JOIN " + DatabaseHelper.TABLE_DETAIL + " d " +
                "ON m." + DatabaseHelper.MAST_CODE + " = d." + DatabaseHelper.EMP_CODE + " " +
                "WHERE m." + DatabaseHelper.EMP_ID + " = ?";

        Cursor cursor = database.rawQuery(query, new String[]{empId});
        Employee employee = null;

        if (cursor.moveToFirst()) {
            employee = new Employee();
            employee.setMastCode(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.MAST_CODE)));
            employee.setEmpId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.EMP_ID)));
            employee.setEmpName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.EMP_NAME)));

            // Handle email safely
            int emailIndex = cursor.getColumnIndex(DatabaseHelper.EMP_EMAIL);
            if (emailIndex != -1 && !cursor.isNull(emailIndex)) {
                employee.setEmpEmail(cursor.getString(emailIndex));
            }

            // Handle password safely
            int passwordIndex = cursor.getColumnIndex(DatabaseHelper.EMP_PASSWORD);
            if (passwordIndex != -1 && !cursor.isNull(passwordIndex)) {
                employee.setEmpPassword(cursor.getString(passwordIndex));
            }

            employee.setDesignation(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DESIGNATION)));
            employee.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEPARTMENT)));
            employee.setJoinedDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.JOINED_DATE)));
            employee.setSalary(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.SALARY)));

            // Handle password changed flag safely
            int passwordChangedIndex = cursor.getColumnIndex(DatabaseHelper.PASSWORD_CHANGED);
            if (passwordChangedIndex != -1) {
                employee.setPasswordChanged(cursor.getInt(passwordChangedIndex) == 1);
            }

            // Load address details if available
            int addressIndex = cursor.getColumnIndex(DatabaseHelper.ADDRESS_LINE1);
            if (addressIndex != -1) {
                employee.setAddressLine1(cursor.getString(addressIndex));
                employee.setAddressLine2(cursor.getString(cursor.getColumnIndex(DatabaseHelper.ADDRESS_LINE2)));
                employee.setCity(cursor.getString(cursor.getColumnIndex(DatabaseHelper.CITY)));
                employee.setState(cursor.getString(cursor.getColumnIndex(DatabaseHelper.STATE)));
                employee.setCountry(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COUNTRY)));
            }
        }

        cursor.close();
        return employee;
    }

    /**
     * Verify employee login with email or employee ID (enhanced)
     */
    public Employee verifyEmployeeLogin(String emailOrId, String password) {
        String hashedPassword = EmployeeCodeGenerator.hashPassword(password);

        String query = "SELECT m.*, d.* FROM " + DatabaseHelper.TABLE_MASTER + " m " +
                "LEFT JOIN " + DatabaseHelper.TABLE_DETAIL + " d " +
                "ON m." + DatabaseHelper.MAST_CODE + " = d." + DatabaseHelper.EMP_CODE + " " +
                "WHERE (m." + DatabaseHelper.EMP_ID + " = ? OR m." + DatabaseHelper.EMP_EMAIL + " = ?) " +
                "AND m." + DatabaseHelper.EMP_PASSWORD + " = ?";

        Cursor cursor = database.rawQuery(query, new String[]{emailOrId, emailOrId, hashedPassword});
        Employee employee = null;

        if (cursor.moveToFirst()) {
            employee = new Employee();
            employee.setMastCode(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.MAST_CODE)));
            employee.setEmpId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.EMP_ID)));
            employee.setEmpName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.EMP_NAME)));

            // Handle email safely
            int emailIndex = cursor.getColumnIndex(DatabaseHelper.EMP_EMAIL);
            if (emailIndex != -1 && !cursor.isNull(emailIndex)) {
                employee.setEmpEmail(cursor.getString(emailIndex));
            }

            employee.setDesignation(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DESIGNATION)));
            employee.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEPARTMENT)));
            employee.setJoinedDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.JOINED_DATE)));
            employee.setSalary(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.SALARY)));

            // Handle password changed safely
            int passwordChangedIndex = cursor.getColumnIndex(DatabaseHelper.PASSWORD_CHANGED);
            if (passwordChangedIndex != -1) {
                employee.setPasswordChanged(cursor.getInt(passwordChangedIndex) == 1);
            }

            // Load address details
            int addressIndex = cursor.getColumnIndex(DatabaseHelper.ADDRESS_LINE1);
            if (addressIndex != -1) {
                employee.setAddressLine1(cursor.getString(addressIndex));
                employee.setAddressLine2(cursor.getString(cursor.getColumnIndex(DatabaseHelper.ADDRESS_LINE2)));
                employee.setCity(cursor.getString(cursor.getColumnIndex(DatabaseHelper.CITY)));
                employee.setState(cursor.getString(cursor.getColumnIndex(DatabaseHelper.STATE)));
                employee.setCountry(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COUNTRY)));
            }
        }

        cursor.close();
        return employee;
    }

    /**
     * Validate old password before changing
     */
    public boolean validateOldPassword(String empId, String oldPassword) {
        String hashedOldPassword = EmployeeCodeGenerator.hashPassword(oldPassword);

        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_MASTER +
                " WHERE " + DatabaseHelper.EMP_ID + " = ? AND " +
                DatabaseHelper.EMP_PASSWORD + " = ?";

        Cursor cursor = database.rawQuery(query, new String[]{empId, hashedOldPassword});
        boolean valid = false;

        if (cursor.moveToFirst()) {
            valid = cursor.getInt(0) > 0;
        }

        cursor.close();
        return valid;
    }

    /**
     * Change employee password (enhanced with plain password storage)
     */
    public boolean changeEmployeePassword(String empId, String newPassword) {
        String hashedPassword = EmployeeCodeGenerator.hashPassword(newPassword);

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.EMP_PASSWORD, hashedPassword);
        values.put(DatabaseHelper.PASSWORD_CHANGED, 1);

        int result = database.update(DatabaseHelper.TABLE_MASTER, values,
                DatabaseHelper.EMP_ID + " = ?", new String[]{empId});

        if (result > 0) {
            // Update plain password storage for admin viewing
            passwordMap.put(empId, newPassword);
            return true;
        }

        return false;
    }

    /**
     * Migrate existing employees without passwords
     */
    private void migrateExistingEmployeePasswords() {
        String query = "SELECT " + DatabaseHelper.MAST_CODE + ", " + DatabaseHelper.EMP_ID + ", " +
                DatabaseHelper.EMP_NAME + ", " + DatabaseHelper.EMP_PASSWORD +
                " FROM " + DatabaseHelper.TABLE_MASTER;

        Cursor cursor = database.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String empId = cursor.getString(1);
                String empName = cursor.getString(2);
                String existingPassword = cursor.getString(3);

                // Generate password for employees without one or store existing ones
                if (existingPassword == null || existingPassword.trim().isEmpty()) {
                    // Generate password for existing employee
                    String plainPassword = EmployeeCodeGenerator.generateInitialPassword(empName, empId);
                    String hashedPassword = EmployeeCodeGenerator.hashPassword(plainPassword);

                    // Store plain password for admin viewing
                    passwordMap.put(empId, plainPassword);

                    // Update database
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.EMP_PASSWORD, hashedPassword);
                    values.put(DatabaseHelper.PASSWORD_CHANGED, 0); // Mark as initial password

                    database.update(DatabaseHelper.TABLE_MASTER, values,
                            DatabaseHelper.MAST_CODE + " = ?",
                            new String[]{String.valueOf(cursor.getInt(0))});
                } else {
                    // For employees with existing passwords, generate a plain password for admin viewing
                    String plainPassword = EmployeeCodeGenerator.generateInitialPassword(empName, empId);
                    passwordMap.put(empId, plainPassword);
                }

            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    /**
     * Validate employee data before insertion
     */
    public ValidationResult validateEmployeeData(Employee employee) {
        List<String> errors = new ArrayList<>();

        // Validate required fields
        if (employee.getEmpName() == null || employee.getEmpName().trim().isEmpty()) {
            errors.add("Employee name is required");
        }

        if (employee.getEmpEmail() == null || employee.getEmpEmail().trim().isEmpty()) {
            errors.add("Email is required");
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(employee.getEmpEmail()).matches()) {
            errors.add("Invalid email format");
        }

        // Check if email already exists (only for new employees)
        if (employee.getMastCode() == 0 && isEmailExists(employee.getEmpEmail())) {
            errors.add("Email address already exists");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Check if email already exists in database
     */
    private boolean isEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_MASTER +
                " WHERE " + DatabaseHelper.EMP_EMAIL + " = ?";
        Cursor cursor = database.rawQuery(query, new String[]{email});

        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }
        cursor.close();
        return exists;
    }

    /**
     * Get All Employees with Details (updated query with new fields)
     */
    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();

        String query = "SELECT m." + DatabaseHelper.MAST_CODE + ", " +
                "m." + DatabaseHelper.EMP_ID + ", " +
                "m." + DatabaseHelper.EMP_NAME + ", " +
                "m." + DatabaseHelper.EMP_EMAIL + ", " +
                "m." + DatabaseHelper.EMP_PASSWORD + ", " +
                "m." + DatabaseHelper.DESIGNATION + ", " +
                "m." + DatabaseHelper.DEPARTMENT + ", " +
                "m." + DatabaseHelper.JOINED_DATE + ", " +
                "m." + DatabaseHelper.SALARY + ", " +
                "m." + DatabaseHelper.PASSWORD_CHANGED + ", " +
                "d." + DatabaseHelper.ADDRESS_LINE1 + ", " +
                "d." + DatabaseHelper.ADDRESS_LINE2 + ", " +
                "d." + DatabaseHelper.CITY + ", " +
                "d." + DatabaseHelper.STATE + ", " +
                "d." + DatabaseHelper.COUNTRY + " " +
                "FROM " + DatabaseHelper.TABLE_MASTER + " m " +
                "LEFT JOIN " + DatabaseHelper.TABLE_DETAIL + " d " +
                "ON m." + DatabaseHelper.MAST_CODE + " = d." + DatabaseHelper.EMP_CODE;

        Cursor cursor = database.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Employee employee = new Employee();
                employee.setMastCode(cursor.getInt(0));
                employee.setEmpId(cursor.getString(1));
                employee.setEmpName(cursor.getString(2));
                employee.setEmpEmail(cursor.getString(3));
                employee.setEmpPassword(cursor.getString(4));
                employee.setDesignation(cursor.getString(5));
                employee.setDepartment(cursor.getString(6));
                employee.setJoinedDate(cursor.getString(7));
                employee.setSalary(cursor.getDouble(8));
                employee.setPasswordChanged(cursor.getInt(9) == 1);
                employee.setAddressLine1(cursor.getString(10));
                employee.setAddressLine2(cursor.getString(11));
                employee.setCity(cursor.getString(12));
                employee.setState(cursor.getString(13));
                employee.setCountry(cursor.getString(14));

                employees.add(employee);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return employees;
    }

    /**
     * Update Employee (existing method updated for new fields)
     */
    public int updateEmployee(Employee employee) {
        database.beginTransaction();
        try {
            // Update Master
            ContentValues masterValues = new ContentValues();
            masterValues.put(DatabaseHelper.EMP_NAME, employee.getEmpName());
            masterValues.put(DatabaseHelper.EMP_EMAIL, employee.getEmpEmail());
            masterValues.put(DatabaseHelper.DESIGNATION, employee.getDesignation());
            masterValues.put(DatabaseHelper.DEPARTMENT, employee.getDepartment());
            masterValues.put(DatabaseHelper.JOINED_DATE, employee.getJoinedDate());
            masterValues.put(DatabaseHelper.SALARY, employee.getSalary());
            // Note: Don't update password here, use separate method

            int masterResult = database.update(DatabaseHelper.TABLE_MASTER, masterValues,
                    DatabaseHelper.MAST_CODE + " = ?",
                    new String[]{String.valueOf(employee.getMastCode())});

            // Update Detail
            ContentValues detailValues = new ContentValues();
            detailValues.put(DatabaseHelper.ADDRESS_LINE1, employee.getAddressLine1());
            detailValues.put(DatabaseHelper.ADDRESS_LINE2, employee.getAddressLine2());
            detailValues.put(DatabaseHelper.CITY, employee.getCity());
            detailValues.put(DatabaseHelper.STATE, employee.getState());
            detailValues.put(DatabaseHelper.COUNTRY, employee.getCountry());

            database.update(DatabaseHelper.TABLE_DETAIL, detailValues,
                    DatabaseHelper.EMP_CODE + " = ?",
                    new String[]{String.valueOf(employee.getMastCode())});

            if (masterResult > 0) {
                database.setTransactionSuccessful();
                return masterResult;
            }
            return 0;
        } finally {
            database.endTransaction();
        }
    }

    // Existing delete method remains unchanged
    public void deleteEmployee(int mastCode) {
        // Get employee ID before deleting to remove from password map
        String empId = null;
        String query = "SELECT " + DatabaseHelper.EMP_ID + " FROM " + DatabaseHelper.TABLE_MASTER +
                " WHERE " + DatabaseHelper.MAST_CODE + " = ?";
        Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(mastCode)});
        if (cursor.moveToFirst()) {
            empId = cursor.getString(0);
        }
        cursor.close();

        // Delete from database (cascade will handle detail table)
        database.delete(DatabaseHelper.TABLE_MASTER,
                DatabaseHelper.MAST_CODE + " = ?",
                new String[]{String.valueOf(mastCode)});

        // Remove from password map
        if (empId != null) {
            passwordMap.remove(empId);
        }
    }

    // Helper classes
    public static class InsertResult {
        private final boolean success;
        private final long mastCode;
        private final String empId;
        private final String generatedPassword;
        private final String message;

        public InsertResult(boolean success, long mastCode, String empId, String generatedPassword, String message) {
            this.success = success;
            this.mastCode = mastCode;
            this.empId = empId;
            this.generatedPassword = generatedPassword;
            this.message = message;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public long getMastCode() { return mastCode; }
        public String getEmpId() { return empId; }
        public String getGeneratedPassword() { return generatedPassword; }
        public String getMessage() { return message; }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }
}
