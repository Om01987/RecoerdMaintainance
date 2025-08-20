package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddEditActivity extends AppCompatActivity {

    private TextInputLayout tilEmpID, tilEmpName, tilEmail, tilDesignation,
            tilDepartment, tilJoinedDate, tilSalary, tilAddress1,
            tilAddress2, tilCity, tilState, tilCountry;

    private TextInputEditText etEmpID, etEmpName, etEmail, etDesignation,
            etDepartment, etJoinedDate, etSalary, etAddress1,
            etAddress2, etCity, etState, etCountry;

    private MaterialButton btnSave;
    private EmployeeRepository repository;
    private String mode;
    private int mastCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        initializeViews();
        repository = new EmployeeRepository(this);
        repository.open();

        mode = getIntent().getStringExtra("mode");
        if ("edit".equals(mode)) {
            mastCode = getIntent().getIntExtra("mastCode", -1);
            loadEmployee(mastCode);
            setTitle("Edit Employee");
        } else {
            setTitle("Add New Employee");
        }

        btnSave.setOnClickListener(v -> saveEmployee());
    }

    private void initializeViews() {
        tilEmpID = findViewById(R.id.tilEmpID);
        tilEmpName = findViewById(R.id.tilEmpName);
        tilEmail = findViewById(R.id.tilEmail);
        tilDesignation = findViewById(R.id.tilDesignation);
        tilDepartment = findViewById(R.id.tilDepartment);
        tilJoinedDate = findViewById(R.id.tilJoinedDate);
        tilSalary = findViewById(R.id.tilSalary);
        tilAddress1 = findViewById(R.id.tilAddress1);
        tilAddress2 = findViewById(R.id.tilAddress2);
        tilCity = findViewById(R.id.tilCity);
        tilState = findViewById(R.id.tilState);
        tilCountry = findViewById(R.id.tilCountry);

        etEmpID = findViewById(R.id.etEmpID);
        etEmpName = findViewById(R.id.etEmpName);
        etEmail = findViewById(R.id.etEmail);
        etDesignation = findViewById(R.id.etDesignation);
        etDepartment = findViewById(R.id.etDepartment);
        etJoinedDate = findViewById(R.id.etJoinedDate);
        etSalary = findViewById(R.id.etSalary);
        etAddress1 = findViewById(R.id.etAddress1);
        etAddress2 = findViewById(R.id.etAddress2);
        etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState);
        etCountry = findViewById(R.id.etCountry);

        btnSave = findViewById(R.id.btnSave);
    }

    private void loadEmployee(int mastCode) {
        for (Employee emp : repository.getAllEmployees()) {
            if (emp.getMastCode() == mastCode) {
                etEmpID.setText(emp.getEmpId());
                etEmpName.setText(emp.getEmpName());
                etEmail.setText(emp.getEmpEmail());
                etDesignation.setText(emp.getDesignation());
                etDepartment.setText(emp.getDepartment());
                etJoinedDate.setText(emp.getJoinedDate());
                etSalary.setText(String.valueOf(emp.getSalary()));
                etAddress1.setText(emp.getAddressLine1());
                etAddress2.setText(emp.getAddressLine2());
                etCity.setText(emp.getCity());
                etState.setText(emp.getState());
                etCountry.setText(emp.getCountry());

                // Show actual employee ID for edit mode
                tilEmpID.setHint("Employee ID");
                break;
            }
        }
    }

    private void saveEmployee() {
        clearErrors();

        // Validate required fields
        if (!validateInputs()) {
            return;
        }

        Employee emp = createEmployeeFromInputs();

        if ("add".equals(mode)) {
            // Validate before insertion
            EmployeeRepository.ValidationResult validation = repository.validateEmployeeData(emp);
            if (!validation.isValid()) {
                showValidationErrors(validation);
                return;
            }

            // Insert employee with auto-generated code and password
            EmployeeRepository.InsertResult result = repository.insertEmployee(emp);

            if (result.isSuccess()) {
                showSuccessDialog(result);
            } else {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            // Update existing employee
            emp.setMastCode(mastCode);
            int result = repository.updateEmployee(emp);

            if (result > 0) {
                Toast.makeText(this, "Employee updated successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to update employee", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validateInputs() {
        boolean valid = true;

        if (TextUtils.isEmpty(etEmpName.getText())) {
            tilEmpName.setError("Employee name is required");
            valid = false;
        }

        if (TextUtils.isEmpty(etEmail.getText())) {
            tilEmail.setError("Email is required");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString()).matches()) {
            tilEmail.setError("Invalid email format");
            valid = false;
        }

        if (TextUtils.isEmpty(etSalary.getText())) {
            tilSalary.setError("Salary is required");
            valid = false;
        }

        return valid;
    }

    private Employee createEmployeeFromInputs() {
        Employee emp = new Employee();
        emp.setEmpName(etEmpName.getText().toString().trim());
        emp.setEmpEmail(etEmail.getText().toString().trim());
        emp.setDesignation(etDesignation.getText().toString().trim());
        emp.setDepartment(etDepartment.getText().toString().trim());
        emp.setJoinedDate(etJoinedDate.getText().toString().trim());

        try {
            emp.setSalary(Double.parseDouble(etSalary.getText().toString().trim()));
        } catch (NumberFormatException e) {
            emp.setSalary(0.0);
        }

        emp.setAddressLine1(etAddress1.getText().toString().trim());
        emp.setAddressLine2(etAddress2.getText().toString().trim());
        emp.setCity(etCity.getText().toString().trim());
        emp.setState(etState.getText().toString().trim());
        emp.setCountry(etCountry.getText().toString().trim());

        return emp;
    }

    private void showValidationErrors(EmployeeRepository.ValidationResult validation) {
        StringBuilder errors = new StringBuilder();
        for (String error : validation.getErrors()) {
            errors.append("• ").append(error).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Validation Error")
                .setMessage(errors.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSuccessDialog(EmployeeRepository.InsertResult result) {
        String message = "Employee added successfully!\n\n" +
                "Employee ID: " + result.getEmpId() + "\n" +
                "Initial Password: " + result.getGeneratedPassword() + "\n\n" +
                "⚠️ Please share these credentials with the employee securely. " +
                "They can change their password after first login.";

        new AlertDialog.Builder(this)
                .setTitle("✅ Success")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void clearErrors() {
        tilEmpName.setError(null);
        tilEmail.setError(null);
        tilSalary.setError(null);
    }

    @Override
    protected void onDestroy() {
        repository.close();
        super.onDestroy();
    }
}
