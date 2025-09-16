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
    private TextInputLayout tilEmpId, tilEmpEmail, tilJoinedDate;
    private TextInputEditText etEmpId, etEmpEmail, etJoinedDate;
    private MaterialButton btnVerify;
    private LinearProgressIndicator progress;

    private AuthRepository authRepository;
    private EmployeeRepository employeeRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authRepository = new AuthRepository(this);
        employeeRepository = new EmployeeRepository(this);
        employeeRepository.open();

        toolbar = findViewById(R.id.toolbar);
        tilEmpId = findViewById(R.id.tilEmpId);
        etEmpId = findViewById(R.id.etEmpId);
        tilEmpEmail = findViewById(R.id.tilEmpEmail);
        etEmpEmail = findViewById(R.id.etEmpEmail);
        tilJoinedDate = findViewById(R.id.tilJoinedDate);
        etJoinedDate = findViewById(R.id.etJoinedDate);
        btnVerify = findViewById(R.id.btnVerify);
        progress = findViewById(R.id.progress);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Forgot Password");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnVerify.setOnClickListener(v -> attemptEmployeeVerify());
    }

    private void attemptEmployeeVerify() {
        tilEmpId.setError(null);
        tilEmpEmail.setError(null);
        tilJoinedDate.setError(null);

        String id = etEmpId.getText().toString().trim();
        String email = etEmpEmail.getText().toString().trim();
        String date = etJoinedDate.getText().toString().trim();

        boolean valid = true;
        if (TextUtils.isEmpty(id)) {
            tilEmpId.setError("Employee ID required");
            valid = false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmpEmail.setError("Valid email required");
            valid = false;
        }
        if (TextUtils.isEmpty(date)) {
            tilJoinedDate.setError("Joined date required");
            valid = false;
        }
        if (!valid) return;

        progress.setVisibility(View.VISIBLE);
        employeeRepository.getEmployeeByEmpId(id, new EmployeeRepository.EmployeeCallback() {
            @Override
            public void onSuccess(Employee employee) {
                progress.setVisibility(View.GONE);
                if (employee.getEmpEmail().equalsIgnoreCase(email)
                        && employee.getJoinedDate().equals(date)) {
                    authRepository.sendPasswordResetEmail(email, new AuthRepository.ResetCallback() {
                        @Override public void onSuccess(String message) {
                            new AlertDialog.Builder(ForgotPasswordActivity.this)
                                    .setTitle("Reset Email Sent")
                                    .setMessage("A reset link was sent to\n" + email)
                                    .setPositiveButton("OK", (d,w)-> finish())
                                    .show();
                        }
                        @Override public void onError(String error) {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Failed to send reset email: " + error,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Verification failed. Check details.",
                            Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onError(String error) {
                progress.setVisibility(View.GONE);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Employee not found", Toast.LENGTH_LONG).show();
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
        if (employeeRepository != null) employeeRepository.close();
        super.onDestroy();
    }
}
