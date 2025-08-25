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
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ForgotPasswordActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnResetPassword;
    private LinearProgressIndicator progress;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authRepository = new AuthRepository(this);

        initializeViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        progress = findViewById(R.id.progress);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Forgot Password");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupClickListeners() {
        btnResetPassword.setOnClickListener(v -> attemptPasswordReset());

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> {
            finish();
        });
    }

    private void attemptPasswordReset() {
        clearErrors();

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (!validateEmail(email)) {
            return;
        }

        showLoading(true);

        // Simulate network delay
        btnResetPassword.postDelayed(() -> {
            // Check if admin exists
            boolean adminExists = authRepository.adminExists(email);

            if (adminExists) {
                showResetSuccessDialog(email);
            } else {
                tilEmail.setError("No account found with this email address");
                showLoading(false);
            }
        }, 1000);
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            return false;
        }

        return true;
    }

    private void clearErrors() {
        tilEmail.setError(null);
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnResetPassword.setEnabled(!loading);
        etEmail.setEnabled(!loading);
    }

    private void showResetSuccessDialog(String email) {
        showLoading(false);

        String message = "Password reset instructions have been sent to:\n\n" + email +
                "\n\nFor demo purposes, you can proceed directly to reset your password.";

        new AlertDialog.Builder(this)
                .setTitle("✅ Reset Email Sent")
                .setMessage(message)
                .setPositiveButton("Reset Password", (dialog, which) -> {
                    Intent intent = new Intent(this, ResetPasswordActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Back to Login", (dialog, which) -> {
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