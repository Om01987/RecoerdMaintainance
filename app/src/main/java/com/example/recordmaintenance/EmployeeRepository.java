package com.example.recordmaintenance;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Firebase-based Employee Repository
 * Replaces SQLite-based employee CRUD with Firebase Realtime Database
 */
public class EmployeeRepository {

    private static final String TAG = "EmployeeRepository";
    private final DatabaseReference mDatabase;
    private final Context context;
    private final EmployeeCodeGenerator codeGenerator;

    public EmployeeRepository(Context context) {
        this.context = context;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.codeGenerator = new EmployeeCodeGenerator(context);
    }

    /**
     * Get all employees (admin only)
     */
    public void getAllEmployees(EmployeeListCallback callback) {
        mDatabase.child("users").orderByChild("role").equalTo("employee")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Employee> employees = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Employee employee = convertToEmployee(snapshot);
                            if (employee != null) {
                                employees.add(employee);
                            }
                        }
                        callback.onSuccess(employees);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onError(databaseError.getMessage());
                    }
                });
    }

    /**
     * Get employee by Firebase UID
     */
    public void getEmployeeByUid(String uid, EmployeeCallback callback) {
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Employee employee = convertToEmployee(dataSnapshot);
                    if (employee != null) {
                        callback.onSuccess(employee);
                    } else {
                        callback.onError("Failed to parse employee data");
                    }
                } else {
                    callback.onError("Employee not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }

    /**
     * Get employee by Employee ID
     */
    public void getEmployeeByEmpId(String empId, EmployeeCallback callback) {
        mDatabase.child("users").orderByChild("empId").equalTo(empId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Should only be one result
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Employee employee = convertToEmployee(snapshot);
                                if (employee != null) {
                                    callback.onSuccess(employee);
                                    return;
                                }
                            }
                        }
                        callback.onError("Employee not found");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onError(databaseError.getMessage());
                    }
                });
    }

    /**
     * Verify employee login (for employee flavor)
     */
    public void verifyEmployeeLogin(String empIdOrEmail, String password, EmployeeLoginCallback callback) {
        // Use Firebase Auth to sign in, then verify it's an employee
        FirebaseAuth.getInstance().signInWithEmailAndPassword(empIdOrEmail, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        getEmployeeByUid(uid, new EmployeeCallback() {
                            @Override
                            public void onSuccess(Employee employee) {
                                if ("employee".equals(employee.getRole())) {
                                    callback.onSuccess(employee);
                                } else {
                                    callback.onError("Access denied: Not an employee account");
                                }
                            }

                            @Override
                            public void onError(String error) {
                                callback.onError("Employee verification failed: " + error);
                            }
                        });
                    } else {
                        // Try with empId if direct email failed
                        if (!empIdOrEmail.contains("@")) {
                            verifyWithEmpId(empIdOrEmail, password, callback);
                        } else {
                            callback.onError("Invalid credentials");
                        }
                    }
                });
    }

    /**
     * Verify login using Employee ID (find email first)
     */
    private void verifyWithEmpId(String empId, String password, EmployeeLoginCallback callback) {
        getEmployeeByEmpId(empId, new EmployeeCallback() {
            @Override
            public void onSuccess(Employee employee) {
                // Try signing in with the found email
                FirebaseAuth.getInstance().signInWithEmailAndPassword(employee.getEmpEmail(), password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                callback.onSuccess(employee);
                            } else {
                                callback.onError("Invalid credentials");
                            }
                        });
            }

            @Override
            public void onError(String error) {
                callback.onError("Employee ID not found");
            }
        });
    }

    /**
     * Create new employee (this will be called from Cloud Function eventually)
     * For now, this is a client-side implementation
     */
    public void createEmployee(Employee employee, String temporaryPassword, CreateEmployeeCallback callback) {
        // Generate employee ID
        String empId = codeGenerator.generateEmployeeCode();
        employee.setEmpId(empId);

        // First create Firebase Auth user
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(employee.getEmpEmail(), temporaryPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();

                        // Create employee data in database
                        Map<String, Object> employeeData = new HashMap<>();
                        employeeData.put("uid", uid);
                        employeeData.put("empId", empId);
                        employeeData.put("name", employee.getEmpName());
                        employeeData.put("email", employee.getEmpEmail());
                        employeeData.put("role", "employee");
                        employeeData.put("designation", employee.getDesignation());
                        employeeData.put("department", employee.getDepartment());
                        employeeData.put("salary", employee.getSalary());
                        employeeData.put("joinedDate", employee.getJoinedDate());
                        employeeData.put("addressLine1", employee.getAddressLine1());
                        employeeData.put("addressLine2", employee.getAddressLine2());
                        employeeData.put("city", employee.getCity());
                        employeeData.put("state", employee.getState());
                        employeeData.put("country", employee.getCountry());
                        employeeData.put("profilePhotoPath", employee.getProfilePhotoPath());
                        employeeData.put("passwordChanged", false);
                        employeeData.put("createdAt", getCurrentTimestamp());
                        employeeData.put("createdBy", FirebaseAuth.getInstance().getCurrentUser().getUid());

                        mDatabase.child("users").child(uid).setValue(employeeData)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        callback.onSuccess(empId, temporaryPassword);
                                    } else {
                                        // Cleanup: delete the auth user if database write failed
                                        task.getResult().getUser().delete();
                                        callback.onError("Failed to save employee data: " +
                                                (dbTask.getException() != null ? dbTask.getException().getMessage() : "Unknown error"));
                                    }
                                });
                    } else {
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Failed to create user account";
                        callback.onError(errorMsg);
                    }
                });
    }

    /**
     * Update employee data
     */
    public void updateEmployee(Employee employee, UpdateCallback callback) {
        if (employee.getUid() == null || employee.getUid().isEmpty()) {
            callback.onError("Employee UID is required for update");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", employee.getEmpName());
        updates.put("email", employee.getEmpEmail());
        updates.put("designation", employee.getDesignation());
        updates.put("department", employee.getDepartment());
        updates.put("salary", employee.getSalary());
        updates.put("joinedDate", employee.getJoinedDate());
        updates.put("addressLine1", employee.getAddressLine1());
        updates.put("addressLine2", employee.getAddressLine2());
        updates.put("city", employee.getCity());
        updates.put("state", employee.getState());
        updates.put("country", employee.getCountry());
        updates.put("profilePhotoPath", employee.getProfilePhotoPath());
        updates.put("updatedAt", getCurrentTimestamp());

        mDatabase.child("users").child(employee.getUid()).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess("Employee updated successfully");
                    } else {
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Failed to update employee";
                        callback.onError(errorMsg);
                    }
                });
    }

    /**
     * Delete employee
     */
    public void deleteEmployee(String uid, DeleteCallback callback) {
        mDatabase.child("users").child(uid).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess("Employee deleted successfully");
                    } else {
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Failed to delete employee";
                        callback.onError(errorMsg);
                    }
                });
    }

    /**
     * Update employee profile photo path
     */
    public void updateEmployeeProfilePhoto(String uid, String photoPath, UpdateCallback callback) {
        mDatabase.child("users").child(uid).child("profilePhotoPath").setValue(photoPath)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess("Profile photo updated");
                    } else {
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Failed to update profile photo";
                        callback.onError(errorMsg);
                    }
                });
    }

    /**
     * Mark password as changed by employee
     */
    public void markPasswordChanged(String uid, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("passwordChanged", true);
        updates.put("passwordChangedAt", getCurrentTimestamp());

        mDatabase.child("users").child(uid).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess("Password status updated");
                    } else {
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Failed to update password status";
                        callback.onError(errorMsg);
                    }
                });
    }

    /**
     * Convert Firebase DataSnapshot to Employee object
     */
    private Employee convertToEmployee(DataSnapshot snapshot) {
        try {
            Employee employee = new Employee();
            employee.setUid(snapshot.getKey());
            employee.setEmpId(snapshot.child("empId").getValue(String.class));
            employee.setEmpName(snapshot.child("name").getValue(String.class));
            employee.setEmpEmail(snapshot.child("email").getValue(String.class));
            employee.setRole(snapshot.child("role").getValue(String.class));
            employee.setDesignation(snapshot.child("designation").getValue(String.class));
            employee.setDepartment(snapshot.child("department").getValue(String.class));

            // Handle salary safely
            Object salaryObj = snapshot.child("salary").getValue();
            if (salaryObj instanceof Number) {
                employee.setSalary(((Number) salaryObj).doubleValue());
            } else {
                employee.setSalary(0.0);
            }

            employee.setJoinedDate(snapshot.child("joinedDate").getValue(String.class));
            employee.setAddressLine1(snapshot.child("addressLine1").getValue(String.class));
            employee.setAddressLine2(snapshot.child("addressLine2").getValue(String.class));
            employee.setCity(snapshot.child("city").getValue(String.class));
            employee.setState(snapshot.child("state").getValue(String.class));
            employee.setCountry(snapshot.child("country").getValue(String.class));
            employee.setProfilePhotoPath(snapshot.child("profilePhotoPath").getValue(String.class));

            // Handle boolean safely
            Boolean passwordChanged = snapshot.child("passwordChanged").getValue(Boolean.class);
            employee.setPasswordChanged(passwordChanged != null ? passwordChanged : false);

            return employee;
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error converting snapshot to Employee", e);
            return null;
        }
    }

    /**
     * Get current timestamp in ISO format
     */
    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
    }

    // Validation methods (keeping from original)
    public ValidationResult validateEmployeeData(Employee employee) {
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();

        if (employee.getEmpName() == null || employee.getEmpName().trim().isEmpty()) {
            errors.add("Employee name is required");
        }

        if (employee.getEmpEmail() == null || employee.getEmpEmail().trim().isEmpty()) {
            errors.add("Email is required");
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(employee.getEmpEmail()).matches()) {
            errors.add("Invalid email format");
        }

        if (employee.getDesignation() == null || employee.getDesignation().trim().isEmpty()) {
            errors.add("Designation is required");
        }

        if (employee.getDepartment() == null || employee.getDepartment().trim().isEmpty()) {
            errors.add("Department is required");
        }

        if (employee.getJoinedDate() == null || employee.getJoinedDate().trim().isEmpty()) {
            errors.add("Joined date is required");
        }

        if (employee.getSalary() <= 0) {
            errors.add("Valid salary is required");
        }

        result.setErrors(errors);
        return result;
    }

    // Callback interfaces
    public interface EmployeeListCallback {
        void onSuccess(List<Employee> employees);
        void onError(String error);
    }

    public interface EmployeeCallback {
        void onSuccess(Employee employee);
        void onError(String error);
    }

    public interface EmployeeLoginCallback {
        void onSuccess(Employee employee);
        void onError(String error);
    }

    public interface CreateEmployeeCallback {
        void onSuccess(String empId, String temporaryPassword);
        void onError(String error);
    }

    public interface UpdateCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // Supporting classes
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }

    public static class InsertResult {
        private boolean success;
        private String empId;
        private String generatedPassword;
        private String message;

        public InsertResult(boolean success, String empId, String password, String message) {
            this.success = success;
            this.empId = empId;
            this.generatedPassword = password;
            this.message = message;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getEmpId() { return empId; }
        public String getGeneratedPassword() { return generatedPassword; }
        public String getMessage() { return message; }
    }

    // Cleanup method
    public void open() {
        // No setup needed for Firebase
    }

    public void close() {
        // No cleanup needed for Firebase
    }
}