package com.example.recordmaintenance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AddEditActivity extends AppCompatActivity {

    private TextInputLayout tilEmpID, tilEmpName, tilEmail, tilDesignation,
            tilDepartment, tilJoinedDate, tilSalary, tilAddress1,
            tilAddress2, tilCity, tilState, tilCountry;

    private TextInputEditText etEmpID, etEmpName, etEmail, etJoinedDate, etSalary,
            etAddress1, etAddress2, etCity, etState, etCountry;

    private AutoCompleteTextView spinnerDesignation, spinnerDepartment;

    private MaterialButton btnSave;
    private EmployeeRepository repository;
    private EmployeeCodeGenerator codeGenerator;
    private String mode;
    private String employeeUid; // Firebase UID for edit mode

    private SharedPreferences customPrefs;
    private static final String CUSTOM_PREFS = "custom_dropdown_prefs";
    private static final String KEY_CUSTOM_DESIGNATIONS = "custom_designations";
    private static final String KEY_CUSTOM_DEPARTMENTS = "custom_departments";

    // Standard dropdown options (keeping same as before)
    private String[] standardDesignations = {
            "Software Engineer", "Senior Software Engineer", "Lead Software Engineer", "Principal Engineer",
            "Software Developer", "Full Stack Developer", "Frontend Developer", "Backend Developer",
            "Mobile App Developer", "Web Developer", "Game Developer", "Embedded Software Engineer",
            "Graduate Engineer Trainee", "Software Engineer Trainee", "Associate Software Engineer",
            "Junior Software Developer", "Intern - Software Development", "Management Trainee",
            "DevOps Engineer", "Site Reliability Engineer (SRE)", "Cloud Engineer", "Platform Engineer",
            "Infrastructure Engineer", "Release Engineer", "Build Engineer", "Automation Engineer",
            "Data Scientist", "Data Analyst", "Data Engineer", "ML Engineer", "AI Engineer",
            "Business Intelligence Analyst", "Big Data Architect", "Analytics Manager", "Data Architect",
            "Quality Assurance Engineer", "Test Automation Engineer", "QA Lead", "Performance Test Engineer",
            "Security Test Engineer", "Manual Tester", "API Test Engineer",
            "Engineering Manager", "Technical Lead", "Team Lead", "Product Manager", "Project Manager",
            "Scrum Master", "Agile Coach", "Director of Engineering", "VP Engineering", "CTO",
            "Cybersecurity Analyst", "Network Engineer", "Database Administrator", "System Administrator",
            "Solutions Architect", "Cloud Architect", "Security Architect", "Enterprise Architect",
            "UX Designer", "UI Designer", "UX/UI Designer", "Product Designer", "Interaction Designer",
            "Visual Designer", "Design System Designer", "User Researcher",
            "Business Analyst", "System Analyst", "Technical Writer", "Technical Documentation Specialist",
            "Business Intelligence Developer", "Product Owner", "Requirements Analyst",
            "Sales Engineer", "Technical Sales Representative", "Customer Success Manager",
            "Solutions Consultant", "Pre-Sales Engineer", "Account Manager",
            "Other (Specify)"
    };

    private String[] standardDepartments = {
            "Engineering", "Software Development", "Platform Engineering", "Infrastructure & DevOps",
            "Quality Assurance", "Data & Analytics", "Cybersecurity", "IT Operations",
            "Product Management", "Product Design", "User Experience (UX)", "User Research",
            "Human Resources", "Finance & Accounting", "Marketing", "Sales", "Customer Success",
            "Business Operations", "Legal & Compliance", "Procurement",
            "Research & Development", "Innovation Lab", "Technical Writing", "Training & Development",
            "Facilities Management", "Vendor Management", "Risk Management",
            "Executive Leadership", "Program Management", "Strategic Planning",
            "Other (Specify)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        customPrefs = getSharedPreferences(CUSTOM_PREFS, MODE_PRIVATE);
        codeGenerator = new EmployeeCodeGenerator(this);

        initializeViews();
        setupDropdowns();
        setupDatePicker();

        repository = new EmployeeRepository(this);
        repository.open();

        mode = getIntent().getStringExtra("mode");
        if ("edit".equals(mode)) {
            employeeUid = getIntent().getStringExtra("employeeUid"); // Changed from mastCode
            if (employeeUid != null) {
                loadEmployee(employeeUid);
                setTitle("Edit Employee");
            } else {
                Toast.makeText(this, "Employee data not found", Toast.LENGTH_SHORT).show();
                finish();
            }
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
        spinnerDesignation = findViewById(R.id.spinnerDesignation);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        etJoinedDate = findViewById(R.id.etJoinedDate);
        etSalary = findViewById(R.id.etSalary);
        etAddress1 = findViewById(R.id.etAddress1);
        etAddress2 = findViewById(R.id.etAddress2);
        etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState);
        etCountry = findViewById(R.id.etCountry);

        btnSave = findViewById(R.id.btnSave);
    }

    private void setupDropdowns() {
        setupDesignationDropdown();
        setupDepartmentDropdown();
    }

    private void setupDesignationDropdown() {
        List<String> designationList = getDesignationList();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, designationList);
        spinnerDesignation.setAdapter(adapter);

        spinnerDesignation.setOnItemClickListener((parent, view, position, id) -> {
            String selected = designationList.get(position);
            if ("Other (Specify)".equals(selected)) {
                showCustomDesignationDialog();
            }
        });
    }

    private void setupDepartmentDropdown() {
        List<String> departmentList = getDepartmentList();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, departmentList);
        spinnerDepartment.setAdapter(adapter);

        spinnerDepartment.setOnItemClickListener((parent, view, position, id) -> {
            String selected = departmentList.get(position);
            if ("Other (Specify)".equals(selected)) {
                showCustomDepartmentDialog();
            }
        });
    }

    private List<String> getDesignationList() {
        Set<String> customDesignations = customPrefs.getStringSet(KEY_CUSTOM_DESIGNATIONS, new HashSet<>());
        List<String> allDesignations = new ArrayList<>();
        allDesignations.addAll(customDesignations);
        allDesignations.addAll(Arrays.asList(standardDesignations));
        return new ArrayList<>(new HashSet<>(allDesignations));
    }

    private List<String> getDepartmentList() {
        Set<String> customDepartments = customPrefs.getStringSet(KEY_CUSTOM_DEPARTMENTS, new HashSet<>());
        List<String> allDepartments = new ArrayList<>();
        allDepartments.addAll(customDepartments);
        allDepartments.addAll(Arrays.asList(standardDepartments));
        return new ArrayList<>(new HashSet<>(allDepartments));
    }

    private void showCustomDesignationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_input, null);
        TextInputLayout tilCustom = dialogView.findViewById(R.id.tilCustomInput);
        TextInputEditText etCustom = dialogView.findViewById(R.id.etCustomInput);

        tilCustom.setHint("Enter Custom Designation");

        new AlertDialog.Builder(this)
                .setTitle("Add Custom Designation")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String custom = etCustom.getText().toString().trim();
                    if (!TextUtils.isEmpty(custom)) {
                        addCustomDesignation(custom);
                        spinnerDesignation.setText(custom);
                        setupDesignationDropdown();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    spinnerDesignation.setText("");
                })
                .show();
    }

    private void showCustomDepartmentDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_input, null);
        TextInputLayout tilCustom = dialogView.findViewById(R.id.tilCustomInput);
        TextInputEditText etCustom = dialogView.findViewById(R.id.etCustomInput);

        tilCustom.setHint("Enter Custom Department");

        new AlertDialog.Builder(this)
                .setTitle("Add Custom Department")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String custom = etCustom.getText().toString().trim();
                    if (!TextUtils.isEmpty(custom)) {
                        addCustomDepartment(custom);
                        spinnerDepartment.setText(custom);
                        setupDepartmentDropdown();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    spinnerDepartment.setText("");
                })
                .show();
    }

    private void addCustomDesignation(String designation) {
        Set<String> customDesignations = new HashSet<>(customPrefs.getStringSet(KEY_CUSTOM_DESIGNATIONS, new HashSet<>()));
        customDesignations.add(designation);
        customPrefs.edit().putStringSet(KEY_CUSTOM_DESIGNATIONS, customDesignations).apply();
    }

    private void addCustomDepartment(String department) {
        Set<String> customDepartments = new HashSet<>(customPrefs.getStringSet(KEY_CUSTOM_DEPARTMENTS, new HashSet<>()));
        customDepartments.add(department);
        customPrefs.edit().putStringSet(KEY_CUSTOM_DEPARTMENTS, customDepartments).apply();
    }

    private void setupDatePicker() {
        etJoinedDate.setOnClickListener(v -> showDatePicker());
        etJoinedDate.setFocusable(false);
        etJoinedDate.setClickable(true);
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Joining Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String formattedDate = sdf.format(new Date(selection));
            etJoinedDate.setText(formattedDate);
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void loadEmployee(String uid) {
        repository.getEmployeeByUid(uid, new EmployeeRepository.EmployeeCallback() {
            @Override
            public void onSuccess(Employee employee) {
                runOnUiThread(() -> {
                    displayEmployeeData(employee);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AddEditActivity.this, "Failed to load employee: " + error, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void displayEmployeeData(Employee employee) {
        etEmpID.setText(employee.getEmpId());
        etEmpName.setText(employee.getEmpName());
        etEmail.setText(employee.getEmpEmail());
        spinnerDesignation.setText(employee.getDesignation());
        spinnerDepartment.setText(employee.getDepartment());
        etJoinedDate.setText(employee.getJoinedDate());
        etSalary.setText(String.valueOf(employee.getSalary()));
        etAddress1.setText(employee.getAddressLine1());
        etAddress2.setText(employee.getAddressLine2());
        etCity.setText(employee.getCity());
        etState.setText(employee.getState());
        etCountry.setText(employee.getCountry());

        tilEmpID.setHint("Employee ID");
    }

    private void saveEmployee() {
        clearErrors();

        if (!validateInputs()) {
            return;
        }

        Employee emp = createEmployeeFromInputs();

        if ("add".equals(mode)) {
            // Generate temporary password
            String tempPassword = EmployeeCodeGenerator.generateInitialPassword(emp.getEmpName(),
                    codeGenerator.generateEmployeeCode());

            // Create employee in Firebase
            repository.createEmployee(emp, tempPassword, new EmployeeRepository.CreateEmployeeCallback() {
                @Override
                public void onSuccess(String empId, String generatedPassword) {
                    runOnUiThread(() -> {
                        showSuccessDialog(empId, generatedPassword);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditActivity.this, "Failed to create employee: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            // Update existing employee
            emp.setUid(employeeUid);
            repository.updateEmployee(emp, new EmployeeRepository.UpdateCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditActivity.this, message, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditActivity.this, "Failed to update employee: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
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

        if (TextUtils.isEmpty(spinnerDesignation.getText())) {
            tilDesignation.setError("Please select a designation");
            valid = false;
        }

        if (TextUtils.isEmpty(spinnerDepartment.getText())) {
            tilDepartment.setError("Please select a department");
            valid = false;
        }

        if (TextUtils.isEmpty(etJoinedDate.getText())) {
            tilJoinedDate.setError("Please select joining date");
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
        emp.setDesignation(spinnerDesignation.getText().toString().trim());
        emp.setDepartment(spinnerDepartment.getText().toString().trim());
        emp.setJoinedDate(etJoinedDate.getText().toString().trim());
        emp.setRole("employee"); // Set role for new employees

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

    private void showSuccessDialog(String empId, String generatedPassword) {
        String message = "✅ Employee created successfully!\n\n" +
                "Employee ID: " + empId + "\n" +
                "Initial Password: " + generatedPassword + "\n\n" +
                "⚠️ Please share these credentials with the employee securely.\n" +
                "The employee can change this password after logging in.";

        new AlertDialog.Builder(this)
                .setTitle("Employee Created")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setNeutralButton("Copy Password", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Employee Password", generatedPassword);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void clearErrors() {
        tilEmpName.setError(null);
        tilEmail.setError(null);
        tilDesignation.setError(null);
        tilDepartment.setError(null);
        tilJoinedDate.setError(null);
        tilSalary.setError(null);
    }

    @Override
    protected void onDestroy() {
        if (repository != null) {
            repository.close();
        }
        super.onDestroy();
    }
}