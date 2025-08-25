package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ResetPasswordActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private MaterialButton btnUpdatePassword;
    private LinearProgressIndicator progress;

    private AuthRepository authRepository;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null) {
            Toast.makeText(this, "Invalid reset request", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        authRepository = new AuthRepository(this);

        initializeViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        progress = findViewById(R.id.progress);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Reset Password");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupClickListeners() {
        btnUpdatePassword.setOnClickListener(v -> attemptPasswordUpdate());
    }

    private void attemptPasswordUpdate() {
        clearErrors();

        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        if (!validatePasswords(newPassword, confirmPassword)) {
            return;
        }

        showLoading(true);

        // Simulate network delay
        btnUpdatePassword.postDelayed(() -> {
            boolean success = authRepository.updateAdminPassword(userEmail, newPassword);

            if (success) {
                showSuccessDialog();
            } else {
                showLoading(false);
                Toast.makeText(this, "Failed to update password. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }

    private boolean validatePasswords(String password, String confirmPassword) {
        boolean valid = true;

        // Validate new password
        if (TextUtils.isEmpty(password)) {
            tilNewPassword.setError("New password is required");
            valid = false;
        } else if (password.length() < 6) {
            tilNewPassword.setError("Password must be at least 6 characters");
            valid = false;
        } else if (!isPasswordStrong(password)) {
            tilNewPassword.setError("Password must contain uppercase, lowercase, and a number");
            valid = false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your new password");
            valid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            valid = false;
        }

        return valid;
    }

    /**
     * Check if password meets strong password criteria
     */
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        return hasUpper && hasLower && hasDigit;
    }

    private void clearErrors() {
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnUpdatePassword.setEnabled(!loading);
        etNewPassword.setEnabled(!loading);
        etConfirmPassword.setEnabled(!loading);
    }

    private void showSuccessDialog() {
        showLoading(false);

        String message = "✅ Password Updated Successfully!\n\n" +
                "Your admin password has been updated securely.\n" +
                "You can now login with your new password.";

        new AlertDialog.Builder(this)
                .setTitle("Password Reset Complete")
                .setMessage(message)
                .setPositiveButton("Login Now", (dialog, which) -> {
                    // Navigate back to login screen
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
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
        super.onDestroy();
    }
}