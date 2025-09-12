package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;
import com.example.recordmaintenance.BuildConfig;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private TextView tvRoleHeader;  // to show login page header according to build variant
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private LinearProgressIndicator progress;

    private AuthRepository authRepository;
    private EmployeeRepository employeeRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository(this);
        employeeRepository = new EmployeeRepository(this);
        employeeRepository.open();

        initViews();

        // Flavor-based UI
        if (BuildConfig.IS_ADMIN) {
            tvRoleHeader.setText("Admin Login");
            tilEmail.setHint("Admin Email");
            etEmail.setText("admin@example.com");
            etPassword.setText("Admin@123");
        } else {
            tvRoleHeader.setText("Employee Login");
            tilEmail.setHint("Employee ID or Email");
            etEmail.setText("");
            etPassword.setText("");
            findViewById(R.id.tvForgot).setVisibility(View.GONE);
        }

        setupActions();
    }

    private void initViews() {
        tvRoleHeader = findViewById(R.id.tvRoleHeader);
        tilEmail      = findViewById(R.id.tilEmail);
        tilPassword   = findViewById(R.id.tilPassword);
        etEmail       = findViewById(R.id.etEmail);
        etPassword    = findViewById(R.id.etPassword);
        btnLogin      = findViewById(R.id.btnLogin);
        progress      = findViewById(R.id.progress);
    }

    private void setupActions() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        etPassword.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
        findViewById(R.id.tvForgot).setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );
    }

    private void attemptLogin() {
        clearErrors();
        String login = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString();
        if (!validate(login, pass)) return;
        showLoading(true);

        btnLogin.postDelayed(() -> {
            if (BuildConfig.IS_ADMIN) {
                attemptAdminLogin(login, pass);
            } else {
                attemptEmployeeLogin(login, pass);
            }
        }, 600);
    }

    private void attemptAdminLogin(String email, String password) {
        if (authRepository.verifyAdmin(email, password)) {
            goToMain();
        } else {
            tilPassword.setError("Invalid admin credentials");
            showLoading(false);
        }
    }

    private void attemptEmployeeLogin(String idOrEmail, String password) {
        Employee e = employeeRepository.verifyEmployeeLogin(idOrEmail, password);
        if (e != null) {
            goToEmployeeProfile(e);
        } else {
            tilPassword.setError("Invalid employee credentials");
            showLoading(false);
        }
    }

    private boolean validate(String login, String pass) {
        boolean ok = true;
        if (TextUtils.isEmpty(login)) {
            tilEmail.setError(BuildConfig.IS_ADMIN
                    ? "Enter admin email"
                    : "Enter employee ID or email");
            ok = false;
        } else if (BuildConfig.IS_ADMIN
                && !Patterns.EMAIL_ADDRESS.matcher(login).matches()) {
            tilEmail.setError("Enter a valid email");
            ok = false;
        }
        if (TextUtils.isEmpty(pass) || pass.length() < 6) {
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
    }

    private void goToMain() {
        showLoading(false);
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("role", "admin");
        startActivity(i);
        finish();
    }

    private void goToEmployeeProfile(Employee e) {
        showLoading(false);
        Intent i = new Intent(this, EmployeeProfileActivity.class);
        i.putExtra("employeeId", e.getEmpId());
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        employeeRepository.close();
        authRepository.close();
        super.onDestroy();
    }
}