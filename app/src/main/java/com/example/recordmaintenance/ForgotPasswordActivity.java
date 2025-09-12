package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ForgotPasswordActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private MaterialButtonToggleGroup toggleUserType;
    private MaterialButton btnAdmin, btnEmployee;

    // Admin fields
    private TextInputLayout tilAdminEmail;
    private TextInputEditText etAdminEmail;

    // Employee fields
    private TextInputLayout tilEmpId, tilEmpEmail, tilJoinedDate;
    private TextInputEditText etEmpId, etEmpEmail, etJoinedDate;

    private MaterialButton btnVerify;
    private LinearProgressIndicator progress;

    private AuthRepository authRepository;
    private EmployeeRepository employeeRepository;

    private enum UserType { ADMIN, EMPLOYEE }
    private UserType selectedUserType = UserType.ADMIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authRepository = new AuthRepository(this);
        employeeRepository = new EmployeeRepository(this);
        employeeRepository.open();

        initializeViews();
        setupToolbar();
        setupToggle();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        toggleUserType = findViewById(R.id.toggleUserType);
        btnAdmin = findViewById(R.id.btnAdmin);
        btnEmployee = findViewById(R.id.btnEmployee);

        // Admin fields
        tilAdminEmail = findViewById(R.id.tilAdminEmail);
        etAdminEmail = findViewById(R.id.etAdminEmail);

        // Employee fields
        tilEmpId = findViewById(R.id.tilEmpId);
        tilEmpEmail = findViewById(R.id.tilEmpEmail);
        tilJoinedDate = findViewById(R.id.tilJoinedDate);
        etEmpId = findViewById(R.id.etEmpId);
        etEmpEmail = findViewById(R.id.etEmpEmail);
        etJoinedDate = findViewById(R.id.etJoinedDate);

        btnVerify = findViewById(R.id.btnVerify);
        progress = findViewById(R.id.progress);

        toggleUserType.check(R.id.btnAdmin);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Forgot Password");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupToggle() {
        toggleUserType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnAdmin) {
                selectedUserType = UserType.ADMIN;
                showAdminFields();
            } else if (checkedId == R.id.btnEmployee) {
                selectedUserType = UserType.EMPLOYEE;
                showEmployeeFields();
            }
        });

        // Initialize with admin fields visible
        showAdminFields();
    }

    private void showAdminFields() {
        // Show admin fields
        tilAdminEmail.setVisibility(View.VISIBLE);

        // Hide employee fields
        tilEmpId.setVisibility(View.GONE);
        tilEmpEmail.setVisibility(View.GONE);
        tilJoinedDate.setVisibility(View.GONE);

        btnVerify.setText("Send Reset Email");
        clearAllFields();
    }

    private void showEmployeeFields() {
        // Hide admin fields
        tilAdminEmail.setVisibility(View.GONE);

        // Show employee fields
        tilEmpId.setVisibility(View.VISIBLE);
        tilEmpEmail.setVisibility(View.VISIBLE);
        tilJoinedDate.setVisibility(View.VISIBLE);

        btnVerify.setText("Verify Employee Details");
        clearAllFields();
    }

    private void clearAllFields() {
        etAdminEmail.setText("");
        etEmpId.setText("");
        etEmpEmail.setText("");
        etJoinedDate.setText("");
        clearErrors();
    }

    private void setupClickListeners() {
        btnVerify.setOnClickListener(v -> {
            if (selectedUserType == UserType.ADMIN) {
                attemptAdminPasswordReset();
            } else {
                attemptEmployeeVerification();
            }
        });

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());
    }

    private void attemptAdminPasswordReset() {
        clearErrors();

        String email = etAdminEmail.getText() != null ? etAdminEmail.getText().toString().trim() : "";

        if (!validateAdminEmail(email)) {
            return;
        }

        showLoading(true);

        // Use Firebase Auth to send password reset email
        authRepository.sendPasswordResetEmail(email, new AuthRepository.ResetCallback() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                showAdminResetSuccessDialog(email);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                // Check if it's a user-not-found error specifically
                if (error.contains("user not found") || error.contains("no user record")) {
                    tilAdminEmail.setError("No admin account found with this email address");
                } else {
                    tilAdminEmail.setError("Failed to send reset email: " + error);
                }
            }
        });
    }

    private void attemptEmployeeVerification() {
        clearErrors();

        String empId = etEmpId.getText() != null ? etEmpId.getText().toString().trim() : "";
        String empEmail = etEmpEmail.getText() != null ? etEmpEmail.getText().toString().trim() : "";
        String joinedDate = etJoinedDate.getText() != null ? etJoinedDate.getText().toString().trim() : "";

        if (!validateEmployeeFields(empId, empEmail, joinedDate)) {
            return;
        }

        showLoading(true);

        // Verify employee details using Firebase database
        verifyEmployeeDetails(empId, empEmail, joinedDate);
    }

    private void verifyEmployeeDetails(String empId, String empEmail, String joinedDate) {
        employeeRepository.getEmployeeByEmpId(empId, new EmployeeRepository.EmployeeCallback() {
            @Override
            public void onSuccess(Employee employee) {
                // Verify email and joined date match
                boolean emailMatches = empEmail.equalsIgnoreCase(employee.getEmpEmail());
                boolean dateMatches = joinedDate.equals(employee.getJoinedDate());

                if (emailMatches && dateMatches) {
                    // Details verified, now send password reset email
                    authRepository.sendPasswordResetEmail(employee.getEmpEmail(), new AuthRepository.ResetCallback() {
                        @Override
                        public void onSuccess(String message) {
                            showLoading(false);
                            showEmployeeResetSuccessDialog(employee);
                        }

                        @Override
                        public void onError(String error) {
                            showLoading(false);
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Failed to send reset email: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    showLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Employee verification failed. Please check your details.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Employee ID not found", Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateAdminEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            tilAdminEmail.setError("Admin email is required");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilAdminEmail.setError("Please enter a valid email address");
            return false;
        }

        return true;
    }

    private boolean validateEmployeeFields(String empId, String empEmail, String joinedDate) {
        boolean valid = true;

        if (TextUtils.isEmpty(empId)) {
            tilEmpId.setError("Employee ID is required");
            valid = false;
        }

        if (TextUtils.isEmpty(empEmail)) {
            tilEmpEmail.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(empEmail).matches()) {
            tilEmpEmail.setError("Please enter a valid email address");
            valid = false;
        }

        if (TextUtils.isEmpty(joinedDate)) {
            tilJoinedDate.setError("Joined date is required (DD/MM/YYYY format)");
            valid = false;
        }

        return valid;
    }

    private void clearErrors() {
        tilAdminEmail.setError(null);
        tilEmpId.setError(null);
        tilEmpEmail.setError(null);
        tilJoinedDate.setError(null);
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnVerify.setEnabled(!loading);
        etAdminEmail.setEnabled(!loading);
        etEmpId.setEnabled(!loading);
        etEmpEmail.setEnabled(!loading);
        etJoinedDate.setEnabled(!loading);
        toggleUserType.setEnabled(!loading);
    }

    private void showAdminResetSuccessDialog(String email) {
        String message = "✅ Password reset email sent successfully!\n\n" +
                "A password reset link has been sent to:\n" + email + "\n\n" +
                "Please check your email and follow the instructions to reset your password.\n\n" +
                "Note: The email may take a few minutes to arrive. Please also check your spam folder.";

        new AlertDialog.Builder(this)
                .setTitle("Reset Email Sent")
                .setMessage(message)
                .setPositiveButton("Back to Login", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showEmployeeResetSuccessDialog(Employee employee) {
        String message = "✅ Employee verified successfully!\n\n" +
                "Employee: " + employee.getEmpName() + "\n" +
                "ID: " + employee.getEmpId() + "\n\n" +
                "A password reset link has been sent to your registered email address:\n" +
                employee.getEmpEmail() + "\n\n" +
                "Please check your email and follow the instructions to reset your password.";

        new AlertDialog.Builder(this)
                .setTitle("Reset Email Sent")
                .setMessage(message)
                .setPositiveButton("Back to Login", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (authRepository != null) {
            authRepository.close();
        }
        if (employeeRepository != null) {
            employeeRepository.close();
        }
        super.onDestroy();
    }
}