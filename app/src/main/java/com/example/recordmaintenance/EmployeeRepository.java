package com.example.recordmaintenance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class EmployeeRepository {
    private static final String TAG = "EmployeeRepository";
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

            Log.d(TAG, "Creating employee with ID: " + empCode + ", Password: " + initialPassword);

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
            masterValues.put(DatabaseHelper.PROFILE_PHOTO_PATH, employee.getProfilePhotoPath());

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
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("EmpEmail") || msg.contains("UNIQUE constraint failed: TblEmployeeMaster.EmpEmail")) {
                return new InsertResult(false, -1, "", "", "Email address already exists");
            } else if (msg.contains("EmpID") || msg.contains("UNIQUE constraint failed: TblEmployeeMaster.EmpID")) {
                return new InsertResult(false, -1, "", "", "Employee ID already exists");
            } else {
                return new InsertResult(false, -1, "", "", "Constraint violation: " + msg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting employee", e);
            return new InsertResult(false, -1, "", "", "Error: " + e.getMessage());
        } finally {
            database.endTransaction();
        }
    }

    public boolean updateEmployeeProfilePhoto(String empId, String photoPath) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.PROFILE_PHOTO_PATH, photoPath);
        int rows = database.update(
                DatabaseHelper.TABLE_MASTER,
                values,
                DatabaseHelper.EMP_ID + " = ?",
                new String[]{empId});
        return rows > 0;
    }

    public String getEmployeePassword(String empId) {
        Log.d(TAG, "Getting password for employee: " + empId);
        if (passwordMap.containsKey(empId)) {
            String password = passwordMap.get(empId);
            Log.d(TAG, "Found password in memory for " + empId + ": " + password);
            return password;
        }
        Employee emp = getEmployeeByEmpId(empId);
        if (emp != null) {
            Log.d(TAG, "Employee found: " + emp.getEmpName() + ", has password: " + (emp.getEmpPassword() != null && !emp.getEmpPassword().trim().isEmpty()));
            if (emp.getEmpPassword() != null && !emp.getEmpPassword().trim().isEmpty()) {
                if (!emp.isPasswordChanged()) {
                    String generatedPassword = EmployeeCodeGenerator.generateInitialPassword(emp.getEmpName(), empId);
                    String generatedHash = EmployeeCodeGenerator.hashPassword(generatedPassword);
                    if (generatedHash.equals(emp.getEmpPassword())) {
                        passwordMap.put(empId, generatedPassword);
                        Log.d(TAG, "Regenerated initial password for " + empId + ": " + generatedPassword);
                        return generatedPassword;
                    }
                }
                return "PASSWORD_CHANGED_BY_EMPLOYEE";
            } else {
                String generatedPassword = EmployeeCodeGenerator.generateInitialPassword(emp.getEmpName(), empId);
                String hashedPassword = EmployeeCodeGenerator.hashPassword(generatedPassword);
                Log.d(TAG, "Generating new password for " + empId + ": " + generatedPassword);
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.EMP_PASSWORD, hashedPassword);
                values.put(DatabaseHelper.PASSWORD_CHANGED, 0);
                database.update(DatabaseHelper.TABLE_MASTER, values,
                        DatabaseHelper.EMP_ID + " = ?", new String[]{empId});
                passwordMap.put(empId, generatedPassword);
                return generatedPassword;
            }
        }
        return "EMPLOYEE_NOT_FOUND";
    }

    public String resetEmployeePassword(String empId) {
        Employee emp = getEmployeeByEmpId(empId);
        if (emp != null) {
            String newPassword = EmployeeCodeGenerator.generateInitialPassword(emp.getEmpName(), empId);
            String hashedPassword = EmployeeCodeGenerator.hashPassword(newPassword);
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.EMP_PASSWORD, hashedPassword);
            values.put(DatabaseHelper.PASSWORD_CHANGED, 0);
            int result = database.update(DatabaseHelper.TABLE_MASTER, values,
                    DatabaseHelper.EMP_ID + " = ?", new String[]{empId});
            if (result > 0) {
                passwordMap.put(empId, newPassword);
                Log.d(TAG, "Reset password for " + empId + ": " + newPassword);
                return newPassword;
            }
        }
        return null;
    }

    public boolean canViewPassword(String empId) {
        Employee emp = getEmployeeByEmpId(empId);
        if (emp != null) {
            return passwordMap.containsKey(empId) || !emp.isPasswordChanged();
        }
        return false;
    }

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
            int emailIndex = cursor.getColumnIndex(DatabaseHelper.EMP_EMAIL);
            if (emailIndex != -1 && !cursor.isNull(emailIndex)) {
                employee.setEmpEmail(cursor.getString(emailIndex));
            }
            int passwordIndex = cursor.getColumnIndex(DatabaseHelper.EMP_PASSWORD);
            if (passwordIndex != -1 && !cursor.isNull(passwordIndex)) {
                employee.setEmpPassword(cursor.getString(passwordIndex));
            }
            int photoIdx = cursor.getColumnIndex(DatabaseHelper.PROFILE_PHOTO_PATH);
            if (photoIdx != -1 && !cursor.isNull(photoIdx)) {
                employee.setProfilePhotoPath(cursor.getString(photoIdx));
            }
            employee.setDesignation(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DESIGNATION)));
            employee.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEPARTMENT)));
            employee.setJoinedDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.JOINED_DATE)));
            employee.setSalary(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.SALARY)));
            int passwordChangedIndex = cursor.getColumnIndex(DatabaseHelper.PASSWORD_CHANGED);
            if (passwordChangedIndex != -1) {
                employee.setPasswordChanged(cursor.getInt(passwordChangedIndex) == 1);
            }
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

    public Employee verifyEmployeeLogin(String emailOrId, String password) {
        Log.d(TAG, "Attempting login for: " + emailOrId);
        String hashedPassword = EmployeeCodeGenerator.hashPassword(password);
        Log.d(TAG, "Hashed password: " + hashedPassword.substring(0, 10) + "...");
        String query = "SELECT m.*, d.* FROM " + DatabaseHelper.TABLE_MASTER + " m " +
                "LEFT JOIN " + DatabaseHelper.TABLE_DETAIL + " d " +
                "ON m." + DatabaseHelper.MAST_CODE + " = d." + DatabaseHelper.EMP_CODE + " " +
                "WHERE (m." + DatabaseHelper.EMP_ID + " = ? OR m." + DatabaseHelper.EMP_EMAIL + " = ?) " +
                "AND m." + DatabaseHelper.EMP_PASSWORD + " = ?";
        Cursor cursor = database.rawQuery(query, new String[]{emailOrId, emailOrId, hashedPassword});
        Employee employee = null;
        Log.d(TAG, "Login query returned " + cursor.getCount() + " results");
        if (cursor.moveToFirst()) {
            employee = new Employee();
            employee.setMastCode(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.MAST_CODE)));
            employee.setEmpId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.EMP_ID)));
            employee.setEmpName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.EMP_NAME)));
            int emailIndex = cursor.getColumnIndex(DatabaseHelper.EMP_EMAIL);
            if (emailIndex != -1 && !cursor.isNull(emailIndex)) {
                employee.setEmpEmail(cursor.getString(emailIndex));
            }
            int photoIdx = cursor.getColumnIndex(DatabaseHelper.PROFILE_PHOTO_PATH);
            if (photoIdx != -1 && !cursor.isNull(photoIdx)) {
                employee.setProfilePhotoPath(cursor.getString(photoIdx));
            }
            employee.setDesignation(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DESIGNATION)));
            employee.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.DEPARTMENT)));
            employee.setJoinedDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.JOINED_DATE)));
            employee.setSalary(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.SALARY)));
            int passwordChangedIndex = cursor.getColumnIndex(DatabaseHelper.PASSWORD_CHANGED);
            if (passwordChangedIndex != -1) {
                employee.setPasswordChanged(cursor.getInt(passwordChangedIndex) == 1);
            }
            int addressIndex = cursor.getColumnIndex(DatabaseHelper.ADDRESS_LINE1);
            if (addressIndex != -1) {
                employee.setAddressLine1(cursor.getString(addressIndex));
                employee.setAddressLine2(cursor.getString(cursor.getColumnIndex(DatabaseHelper.ADDRESS_LINE2)));
                employee.setCity(cursor.getString(cursor.getColumnIndex(DatabaseHelper.CITY)));
                employee.setState(cursor.getString(cursor.getColumnIndex(DatabaseHelper.STATE)));
                employee.setCountry(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COUNTRY)));
            }
        } else {
            Log.d(TAG, "Login failed for: " + emailOrId);
        }
        cursor.close();
        return employee;
    }

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

    public boolean changeEmployeePassword(String empId, String newPassword) {
        String hashedPassword = EmployeeCodeGenerator.hashPassword(newPassword);
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.EMP_PASSWORD, hashedPassword);
        values.put(DatabaseHelper.PASSWORD_CHANGED, 1);
        int result = database.update(DatabaseHelper.TABLE_MASTER, values,
                DatabaseHelper.EMP_ID + " = ?", new String[]{empId});
        if (result > 0) {
            passwordMap.remove(empId);
            Log.d(TAG, "Password changed for employee: " + empId);
            return true;
        }
        return false;
    }

    private void migrateExistingEmployeePasswords() {
        Log.d(TAG, "Starting password migration...");
        String query = "SELECT " + DatabaseHelper.MAST_CODE + ", " + DatabaseHelper.EMP_ID + ", " +
                DatabaseHelper.EMP_NAME + ", " + DatabaseHelper.EMP_PASSWORD +
                " FROM " + DatabaseHelper.TABLE_MASTER;
        Cursor cursor = database.rawQuery(query, null);
        int migrated = 0;
        if (cursor.moveToFirst()) {
            do {
                String empId = cursor.getString(1);
                String empName = cursor.getString(2);
                String existingPassword = cursor.getString(3);
                if (existingPassword == null || existingPassword.trim().isEmpty()) {
                    String plainPassword = EmployeeCodeGenerator.generateInitialPassword(empName, empId);
                    String hashedPassword = EmployeeCodeGenerator.hashPassword(plainPassword);
                    passwordMap.put(empId, plainPassword);
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.EMP_PASSWORD, hashedPassword);
                    values.put(DatabaseHelper.PASSWORD_CHANGED, 0);
                    database.update(DatabaseHelper.TABLE_MASTER, values,
                            DatabaseHelper.MAST_CODE + " = ?",
                            new String[]{String.valueOf(cursor.getInt(0))});
                    migrated++;
                    Log.d(TAG, "Generated password for existing employee: " + empId + " -> " + plainPassword);
                } else {
                    String potentialInitialPassword = EmployeeCodeGenerator.generateInitialPassword(empName, empId);
                    String potentialHash = EmployeeCodeGenerator.hashPassword(potentialInitialPassword);
                    if (potentialHash.equals(existingPassword)) {
                        passwordMap.put(empId, potentialInitialPassword);
                        Log.d(TAG, "Recovered initial password for existing employee: " + empId);
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        Log.d(TAG, "Migration completed. Updated " + migrated + " employees with new passwords.");
    }

    public PasswordStatistics getPasswordStatistics() {
        String query = "SELECT " +
                "COUNT(*) as total, " +
                "SUM(CASE WHEN " + DatabaseHelper.PASSWORD_CHANGED + " = 0 THEN 1 ELSE 0 END) as initial, " +
                "SUM(CASE WHEN " + DatabaseHelper.PASSWORD_CHANGED + " = 1 THEN 1 ELSE 0 END) as changed " +
                "FROM " + DatabaseHelper.TABLE_MASTER +
                " WHERE " + DatabaseHelper.EMP_PASSWORD + " IS NOT NULL AND " + DatabaseHelper.EMP_PASSWORD + " != ''";
        Cursor cursor = database.rawQuery(query, null);
        PasswordStatistics stats = new PasswordStatistics();
        if (cursor.moveToFirst()) {
            stats.totalEmployees = cursor.getInt(0);
            stats.initialPasswords = cursor.getInt(1);
            stats.changedPasswords = cursor.getInt(2);
            stats.viewablePasswords = passwordMap.size();
        }
        cursor.close();
        return stats;
    }

    public ValidationResult validateEmployeeData(Employee employee) {
        List<String> errors = new ArrayList<>();
        if (employee.getEmpName() == null || employee.getEmpName().trim().isEmpty()) {
            errors.add("Employee name is required");
        }
        if (employee.getEmpEmail() == null || employee.getEmpEmail().trim().isEmpty()) {
            errors.add("Email is required");
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(employee.getEmpEmail()).matches()) {
            errors.add("Invalid email format");
        }
        if (employee.getMastCode() == 0 && isEmailExists(employee.getEmpEmail())) {
            errors.add("Email address already exists");
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

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
                "m." + DatabaseHelper.PROFILE_PHOTO_PATH + ", " +
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
                employee.setProfilePhotoPath(cursor.getString(10));
                employee.setAddressLine1(cursor.getString(11));
                employee.setAddressLine2(cursor.getString(12));
                employee.setCity(cursor.getString(13));
                employee.setState(cursor.getString(14));
                employee.setCountry(cursor.getString(15));
                employees.add(employee);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return employees;
    }

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
            masterValues.put(DatabaseHelper.PROFILE_PHOTO_PATH, employee.getProfilePhotoPath());

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

    public void deleteEmployee(int mastCode) {
        String empId = null;
        String query = "SELECT " + DatabaseHelper.EMP_ID + " FROM " + DatabaseHelper.TABLE_MASTER +
                " WHERE " + DatabaseHelper.MAST_CODE + " = ?";
        Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(mastCode)});
        if (cursor.moveToFirst()) {
            empId = cursor.getString(0);
        }
        cursor.close();
        database.delete(DatabaseHelper.TABLE_MASTER,
                DatabaseHelper.MAST_CODE + " = ?",
                new String[]{String.valueOf(mastCode)});
        if (empId != null) {
            passwordMap.remove(empId);
        }
    }

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

    public static class PasswordStatistics {
        public int totalEmployees = 0;
        public int initialPasswords = 0;
        public int changedPasswords = 0;
        public int viewablePasswords = 0;
    }
}
