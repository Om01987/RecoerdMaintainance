package com.example.recordmaintenance;

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
    private MaterialButton btnSendReset;
    private LinearProgressIndicator progress;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        toolbar = findViewById(R.id.toolbar);
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        progress = findViewById(R.id.progress);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Forgot Password");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        authRepository = new AuthRepository(this);

        btnSendReset.setOnClickListener(v -> attemptPasswordReset());
    }

    private void attemptPasswordReset() {
        tilEmail.setError(null);
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            return;
        }

        progress.setVisibility(View.VISIBLE);
        btnSendReset.setEnabled(false);

        authRepository.sendPasswordResetEmail(email, new AuthRepository.ResetCallback() {
            @Override
            public void onSuccess(String message) {
                progress.setVisibility(View.GONE);
                btnSendReset.setEnabled(true);
                new AlertDialog.Builder(ForgotPasswordActivity.this)
                        .setTitle("Reset Email Sent")
                        .setMessage("If an account exists for:\n" + email +
                                "\n\nYou will receive a reset link shortly. Please check your inbox and spam folder.")
                        .setPositiveButton("OK", (d, w) -> finish())
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onError(String error) {
                progress.setVisibility(View.GONE);
                btnSendReset.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Failed to send reset email: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (authRepository != null) authRepository.close();
        super.onDestroy();
    }
}
