package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class AddEditActivity extends AppCompatActivity {

    private EditText etEmpID, etEmpName, etDesignation, etDepartment,
            etJoinedDate, etSalary, etAddress1, etAddress2, etCity, etState, etCountry;
    private Button btnSave;

    private EmployeeRepository repository;
    private String mode;
    private int mastCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        // Initialize views
        etEmpID = findViewById(R.id.etEmpID);
        etEmpName = findViewById(R.id.etEmpName);
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

        repository = new EmployeeRepository(this);
        repository.open();

        // Get mode (add/edit)
        mode = getIntent().getStringExtra("mode");
        if ("edit".equals(mode)) {
            mastCode = getIntent().getIntExtra("mastCode", -1);
            loadEmployee(mastCode);
        }

        btnSave.setOnClickListener(v -> saveEmployee());
    }

    private void loadEmployee(int mastCode) {
        // Fetch all and find matching mastCode (simplest for demo)
        for (Employee emp : repository.getAllEmployees()) {
            if (emp.getMastCode() == mastCode) {
                etEmpID.setText(emp.getEmpId());
                etEmpName.setText(emp.getEmpName());
                etDesignation.setText(emp.getDesignation());
                etDepartment.setText(emp.getDepartment());
                etJoinedDate.setText(emp.getJoinedDate());
                etSalary.setText(String.valueOf(emp.getSalary()));
                etAddress1.setText(emp.getAddressLine1());
                etAddress2.setText(emp.getAddressLine2());
                etCity.setText(emp.getCity());
                etState.setText(emp.getState());
                etCountry.setText(emp.getCountry());
                break;
            }
        }
    }

    private void saveEmployee() {
        // Validate required fields
        if (TextUtils.isEmpty(etEmpID.getText()) ||
                TextUtils.isEmpty(etEmpName.getText()) ||
                TextUtils.isEmpty(etSalary.getText())) {
            etEmpName.setError("Required");
            return;
        }

        Employee emp = new Employee();
        emp.setEmpId(etEmpID.getText().toString().trim());
        emp.setEmpName(etEmpName.getText().toString().trim());
        emp.setDesignation(etDesignation.getText().toString().trim());
        emp.setDepartment(etDepartment.getText().toString().trim());
        emp.setJoinedDate(etJoinedDate.getText().toString().trim());
        emp.setSalary(Double.parseDouble(etSalary.getText().toString().trim()));
        emp.setAddressLine1(etAddress1.getText().toString().trim());
        emp.setAddressLine2(etAddress2.getText().toString().trim());
        emp.setCity(etCity.getText().toString().trim());
        emp.setState(etState.getText().toString().trim());
        emp.setCountry(etCountry.getText().toString().trim());

        if ("add".equals(mode)) {
            repository.insertEmployee(emp);
        } else {
            emp.setMastCode(mastCode);
            repository.updateEmployee(emp);
        }

        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        repository.close();
        super.onDestroy();
    }
}
