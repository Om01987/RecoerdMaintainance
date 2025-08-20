package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class EmployeeProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CHANGE_PASSWORD = 200;

    // UI Components
    private MaterialToolbar toolbar;
    private TextView tvEmployeeName, tvEmployeeId, tvEmail, tvDesignation,
            tvDepartment, tvSalary, tvJoinedDate, tvPasswordStatus;
    private TextView tvAddressLine1, tvAddressLine2, tvCity, tvState, tvCountry;
    private MaterialButton btnChangePassword;
    private CardView cvPersonalInfo, cvJobInfo, cvAddressInfo, cvSecurityInfo;

    // Data Components
    private EmployeeRepository repository;
    private Employee currentEmployee;
    private String employeeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_profile);

        employeeId = getIntent().getStringExtra("employeeId");
        if (employeeId == null) {
            Toast.makeText(this, "Employee ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository = new EmployeeRepository(this);
        repository.open();

        initializeViews();
        setupToolbar();
        loadEmployeeData();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);

        // Personal Info
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvEmail = findViewById(R.id.tvEmail);

        // Job Info
        tvDesignation = findViewById(R.id.tvDesignation);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvSalary = findViewById(R.id.tvSalary);
        tvJoinedDate = findViewById(R.id.tvJoinedDate);

        // Address Info
        tvAddressLine1 = findViewById(R.id.tvAddressLine1);
        tvAddressLine2 = findViewById(R.id.tvAddressLine2);
        tvCity = findViewById(R.id.tvCity);
        tvState = findViewById(R.id.tvState);
        tvCountry = findViewById(R.id.tvCountry);

        // Security Info
        tvPasswordStatus = findViewById(R.id.tvPasswordStatus);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Card Views
        cvPersonalInfo = findViewById(R.id.cvPersonalInfo);
        cvJobInfo = findViewById(R.id.cvJobInfo);
        cvAddressInfo = findViewById(R.id.cvAddressInfo);
        cvSecurityInfo = findViewById(R.id.cvSecurityInfo);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadEmployeeData() {
        currentEmployee = repository.getEmployeeByEmpId(employeeId);

        if (currentEmployee != null) {
            displayEmployeeData();
        } else {
            Toast.makeText(this, "Employee data not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayEmployeeData() {
        // Personal Info
        tvEmployeeName.setText(currentEmployee.getEmpName());
        tvEmployeeId.setText(currentEmployee.getEmpId());
        tvEmail.setText(currentEmployee.getEmpEmail() != null ? currentEmployee.getEmpEmail() : "Not provided");

        // Job Info
        tvDesignation.setText(currentEmployee.getDesignation() != null ? currentEmployee.getDesignation() : "Not assigned");
        tvDepartment.setText(currentEmployee.getDepartment() != null ? currentEmployee.getDepartment() : "Not assigned");
        tvSalary.setText("₹" + String.valueOf(currentEmployee.getSalary()));
        tvJoinedDate.setText(currentEmployee.getJoinedDate() != null ? currentEmployee.getJoinedDate() : "Not available");

        // Address Info
        tvAddressLine1.setText(currentEmployee.getAddressLine1() != null ? currentEmployee.getAddressLine1() : "Not provided");
        tvAddressLine2.setText(currentEmployee.getAddressLine2() != null ? currentEmployee.getAddressLine2() : "Not provided");
        tvCity.setText(currentEmployee.getCity() != null ? currentEmployee.getCity() : "Not provided");
        tvState.setText(currentEmployee.getState() != null ? currentEmployee.getState() : "Not provided");
        tvCountry.setText(currentEmployee.getCountry() != null ? currentEmployee.getCountry() : "Not provided");

        // Security Info
        tvPasswordStatus.setText(currentEmployee.isPasswordChanged() ?
                "Password has been changed" : "Using initial password");

        // Update toolbar title with employee name
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentEmployee.getEmpName() + "'s Profile");
        }
    }

    private void setupClickListeners() {
        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            intent.putExtra("employeeId", employeeId);
            startActivityForResult(intent, REQUEST_CHANGE_PASSWORD);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_logout) {
            showLogoutConfirmation();
            return true;
        } else if (id == R.id.action_refresh) {
            loadEmployeeData();
            Toast.makeText(this, "Profile refreshed", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    repository.close();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_lock_power_off)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHANGE_PASSWORD && resultCode == RESULT_OK) {
            // Refresh employee data to update password status
            loadEmployeeData();
            Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (repository != null) {
            repository.close();
        }
        super.onDestroy();
    }
}
