package com.example.recordmaintenance;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePasswordActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout tilOldPassword, tilNewPassword, tilConfirmPassword;
    private TextInputEditText etOldPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnChangePassword;
    private LinearProgressIndicator progressIndicator;

    private AuthRepository authRepository;
    private EmployeeRepository employeeRepository;
    private String employeeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        employeeId = getIntent().getStringExtra("employeeId");
        if (employeeId == null) {
            Toast.makeText(this, "Employee ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        authRepository = new AuthRepository(this);
        employeeRepository = new EmployeeRepository(this);
        employeeRepository.open();

        initializeViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tilOldPassword = findViewById(R.id.tilOldPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Add progress indicator if it exists in layout
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Change Password");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupClickListeners() {
        btnChangePassword.setOnClickListener(v -> attemptPasswordChange());
    }

    private void attemptPasswordChange() {
        clearErrors();

        String oldPassword = etOldPassword.getText() != null ? etOldPassword.getText().toString() : "";
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        if (!validateInputs(oldPassword, newPassword, confirmPassword)) {
            return;
        }

        showLoading(true);

        // Step 1: Re-authenticate the user with current password
        authRepository.reauthenticateUser(oldPassword, new AuthRepository.ReauthCallback() {
            @Override
            public void onSuccess() {
                // Step 2: Update the password
                authRepository.updatePassword(newPassword, new AuthRepository.UpdateCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Step 3: Mark password as changed in database
                        String currentUserUid = authRepository.getCurrentUserUid();
                        if (currentUserUid != null) {
                            employeeRepository.markPasswordChanged(currentUserUid, new EmployeeRepository.UpdateCallback() {
                                @Override
                                public void onSuccess(String dbMessage) {
                                    showLoading(false);
                                    runOnUiThread(() -> {
                                        Toast.makeText(ChangePasswordActivity.this,
                                                "Password changed successfully!", Toast.LENGTH_SHORT).show();
                                        setResult(RESULT_OK);
                                        finish();
                                    });
                                }

                                @Override
                                public void onError(String dbError) {
                                    showLoading(false);
                                    runOnUiThread(() -> {
                                        // Password was updated in Auth but failed to update DB status
                                        Toast.makeText(ChangePasswordActivity.this,
                                                "Password changed, but failed to update status: " + dbError,
                                                Toast.LENGTH_LONG).show();
                                        setResult(RESULT_OK);
                                        finish();
                                    });
                                }
                            });
                        } else {
                            showLoading(false);
                            runOnUiThread(() -> {
                                Toast.makeText(ChangePasswordActivity.this,
                                        "Password changed successfully!", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        runOnUiThread(() -> {
                            if (error.contains("weak-password")) {
                                tilNewPassword.setError("Password is too weak. Please choose a stronger password.");
                            } else {
                                Toast.makeText(ChangePasswordActivity.this,
                                        "Failed to update password: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                runOnUiThread(() -> {
                    if (error.contains("wrong-password") || error.contains("invalid-credential")) {
                        tilOldPassword.setError("Current password is incorrect");
                    } else if (error.contains("too-many-requests")) {
                        Toast.makeText(ChangePasswordActivity.this,
                                "Too many failed attempts. Please try again later.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ChangePasswordActivity.this,
                                "Authentication failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private boolean validateInputs(String oldPassword, String newPassword, String confirmPassword) {
        boolean valid = true;

        // Validate old password
        if (TextUtils.isEmpty(oldPassword)) {
            tilOldPassword.setError("Current password is required");
            valid = false;
        }

        // Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            tilNewPassword.setError("New password is required");
            valid = false;
        } else if (newPassword.length() < 6) {
            tilNewPassword.setError("Password must be at least 6 characters");
            valid = false;
        } else if (newPassword.equals(oldPassword)) {
            tilNewPassword.setError("New password must be different from current password");
            valid = false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your new password");
            valid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            valid = false;
        }

        return valid;
    }

    private void showLoading(boolean loading) {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        }

        btnChangePassword.setEnabled(!loading);
        etOldPassword.setEnabled(!loading);
        etNewPassword.setEnabled(!loading);
        etConfirmPassword.setEnabled(!loading);

        if (loading) {
            btnChangePassword.setText("Changing Password...");
        } else {
            btnChangePassword.setText("Change Password");
        }
    }

    private void clearErrors() {
        tilOldPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
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