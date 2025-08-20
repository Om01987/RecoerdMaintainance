package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup toggleRole;
    private MaterialButton btnAdmin, btnEmployee, btnLogin;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private LinearProgressIndicator progress;

    private AuthRepository authRepository;
    private EmployeeRepository employeeRepository;

    private enum Role { ADMIN, EMPLOYEE }
    private Role selectedRole = Role.ADMIN;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository(this);
        employeeRepository = new EmployeeRepository(this);
        employeeRepository.open();

        initViews();
        setupToggle();
        setupActions();
        presetDemo();
    }

    private void initViews() {
        toggleRole = findViewById(R.id.toggleRole);
        btnAdmin = findViewById(R.id.btnAdmin);
        btnEmployee = findViewById(R.id.btnEmployee);
        btnLogin = findViewById(R.id.btnLogin);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progress = findViewById(R.id.progress);

        toggleRole.check(R.id.btnAdmin);
    }

    private void setupToggle() {
        toggleRole.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnAdmin) {
                selectedRole = Role.ADMIN;
                tilEmail.setHint("Admin Email");
                // Set admin defaults
                etEmail.setText("admin@example.com");
                etPassword.setText("Admin@123");
            } else if (checkedId == R.id.btnEmployee) {
                selectedRole = Role.EMPLOYEE;
                tilEmail.setHint("Employee ID or Email");
                // Clear for employee input
                etEmail.setText("");
                etPassword.setText("");
            }
        });
    }

    private void setupActions() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        findViewById(R.id.tvForgot).setOnClickListener(v ->
                Toast.makeText(this, "Contact admin for password reset.", Toast.LENGTH_LONG).show()
        );
    }

    private void presetDemo() {
        etEmail.setText("admin@example.com");
        etPassword.setText("Admin@123");
    }

    private void attemptLogin() {
        clearErrors();

        String emailOrId = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        boolean valid = validate(emailOrId, password);
        if (!valid) return;

        showLoading(true);

        btnLogin.postDelayed(() -> {
            if (selectedRole == Role.ADMIN) {
                attemptAdminLogin(emailOrId, password);
            } else {
                attemptEmployeeLogin(emailOrId, password);
            }
        }, 600);
    }

    private void attemptAdminLogin(String email, String password) {
        boolean success = authRepository.verifyAdmin(email, password);
        if (success) {
            goToMain("admin", null);
        } else {
            tilPassword.setError("Invalid admin credentials");
            showLoading(false);
        }
    }

    private void attemptEmployeeLogin(String emailOrId, String password) {
        Employee employee = employeeRepository.verifyEmployeeLogin(emailOrId, password);
        if (employee != null) {
            goToEmployeeProfile(employee);
        } else {
            tilPassword.setError("Invalid employee credentials");
            showLoading(false);
        }
    }

    private boolean validate(String emailOrId, String password) {
        boolean ok = true;

        if (TextUtils.isEmpty(emailOrId)) {
            if (selectedRole == Role.ADMIN) {
                tilEmail.setError("Enter admin email");
            } else {
                tilEmail.setError("Enter employee ID or email");
            }
            ok = false;
        } else if (selectedRole == Role.ADMIN && !Patterns.EMAIL_ADDRESS.matcher(emailOrId).matches()) {
            tilEmail.setError("Enter a valid email");
            ok = false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            ok = false;
        }

        return ok;
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        toggleRole.setEnabled(!loading);
    }

    private void goToMain(String role, String employeeId) {
        showLoading(false);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("role", role);
        if (employeeId != null) {
            intent.putExtra("employeeId", employeeId);
        }
        startActivity(intent);
        finish();
    }

    private void goToEmployeeProfile(Employee employee) {
        showLoading(false);
        Intent intent = new Intent(this, EmployeeProfileActivity.class);
        intent.putExtra("employeeId", employee.getEmpId());
        intent.putExtra("employeeName", employee.getEmpName());
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (employeeRepository != null) {
            employeeRepository.close();
        }
        super.onDestroy();
    }
}
