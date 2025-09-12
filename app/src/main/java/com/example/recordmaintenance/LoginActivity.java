package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // Match the XML IDs exactly
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private TextView tvRoleHeader;
    private MaterialButton btnLogin;
    private TextView tvForgot;
    private LinearProgressIndicator progress;

    private AuthRepository authRepository;
    private boolean isAdminMode = true; // Default to admin mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository(this);

        // Check if user is already signed in
        if (authRepository.isUserSignedIn()) {
            getCurrentUserRoleAndRedirect();
            return;
        }

        initializeViews();
        setupClickListeners();
        updateUIForRole();
    }

    private void getCurrentUserRoleAndRedirect() {
        String currentUid = authRepository.getCurrentUserUid();
        if (currentUid != null) {
            showLoading(true);

            authRepository.getUserRole(currentUid, new AuthRepository.RoleCallback() {
                @Override
                public void onRoleRetrieved(String role, String empId) {
                    showLoading(false);
                    redirectBasedOnRole(role, empId);
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    authRepository.signOut();
                    Log.e(TAG, "Failed to get user role: " + error);
                    Toast.makeText(LoginActivity.this, "Please sign in again", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initializeViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvRoleHeader = findViewById(R.id.tvRoleHeader);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgot = findViewById(R.id.tvForgot);
        progress = findViewById(R.id.progress);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvForgot.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Toggle between admin and employee with header tap
        tvRoleHeader.setOnClickListener(v -> {
            isAdminMode = !isAdminMode;
            updateUIForRole();
        });
    }

    private void updateUIForRole() {
        if (isAdminMode) {
            tvRoleHeader.setText("🔐 Admin Login");
            tilEmail.setHint("Admin Email");
            etEmail.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        } else {
            tvRoleHeader.setText("👤 Employee Login");
            tilEmail.setHint("Employee ID or Email");
            etEmail.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        }
        clearErrors();
    }

    private void attemptLogin() {
        clearErrors();

        String userInput = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (!validateInputs(userInput, password)) {
            return;
        }

        showLoading(true);

        if (isAdminMode) {
            // Admin login - direct Firebase Auth
            authRepository.signIn(userInput, password, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess(String role, String empId) {
                    showLoading(false);
                    if ("admin".equals(role)) {
                        redirectBasedOnRole(role, empId);
                    } else {
                        authRepository.signOut();
                        showError("Access denied: Admin account required");
                    }
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    showError("Login failed: " + error);
                }
            });
        } else {
            // Employee login - can use empId or email
            if (Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
                // Direct email login
                authRepository.signIn(userInput, password, new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(String role, String empId) {
                        showLoading(false);
                        if ("employee".equals(role)) {
                            redirectBasedOnRole(role, empId);
                        } else {
                            authRepository.signOut();
                            showError("Access denied: Employee account required");
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        showError("Login failed: " + error);
                    }
                });
            } else {
                // Employee ID login - need to find email first
                EmployeeRepository empRepo = new EmployeeRepository(this);
                empRepo.getEmployeeByEmpId(userInput, new EmployeeRepository.EmployeeCallback() {
                    @Override
                    public void onSuccess(Employee employee) {
                        authRepository.signIn(employee.getEmpEmail(), password, new AuthRepository.AuthCallback() {
                            @Override
                            public void onSuccess(String role, String empId) {
                                showLoading(false);
                                if ("employee".equals(role)) {
                                    redirectBasedOnRole(role, empId);
                                } else {
                                    authRepository.signOut();
                                    showError("Access denied: Employee account required");
                                }
                            }

                            @Override
                            public void onError(String error) {
                                showLoading(false);
                                showError("Invalid credentials");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        showError("Employee ID not found");
                    }
                });
            }
        }
    }

    private boolean validateInputs(String userInput, String password) {
        boolean valid = true;

        if (TextUtils.isEmpty(userInput)) {
            tilEmail.setError(isAdminMode ? "Email is required" : "Employee ID or Email is required");
            valid = false;
        } else if (isAdminMode && !Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
            tilEmail.setError("Please enter a valid email address");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            valid = false;
        }

        return valid;
    }

    private void redirectBasedOnRole(String role, String empId) {
        Intent intent;
        if ("admin".equals(role)) {
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("role", "admin");
        } else if ("employee".equals(role)) {
            intent = new Intent(this, EmployeeProfileActivity.class);
            intent.putExtra("role", "employee");
            intent.putExtra("employeeId", empId);
        } else {
            showError("Unknown user role");
            return;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean loading) {
        if (progress != null) {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }

        btnLogin.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        tvRoleHeader.setEnabled(!loading);
        tvForgot.setEnabled(!loading);

        if (loading) {
            btnLogin.setText("Signing in...");
        } else {
            btnLogin.setText("Log In");
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    @Override
    protected void onDestroy() {
        if (authRepository != null) {
            authRepository.close();
        }
        super.onDestroy();
    }
}