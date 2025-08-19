package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ADD = 100;
    private static final int REQUEST_EDIT = 101;

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ExtendedFloatingActionButton fabAdd;
    private TextView tvLoggedInAs;
    private TextView tvTotalEmployees;
    private TextView tvLastUpdated;
    private View emptyStateLayout;

    // Data Components
    private EmployeeRepository repository;
    private EmployeeAdapter adapter;
    private String currentRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize data
        currentRole = getIntent().getStringExtra("role");
        if (currentRole == null) currentRole = "admin";

        repository = new EmployeeRepository(this);
        repository.open();

        // Initialize UI
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFloatingActionButton();

        // Load data
        loadEmployeeData();
        updateLastRefreshTime();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        fabAdd = findViewById(R.id.fabAdd);
        tvLoggedInAs = findViewById(R.id.tvLoggedInAs);
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Employee Management System");
        }

        // Update login status
        tvLoggedInAs.setText(currentRole.substring(0, 1).toUpperCase() +
                currentRole.substring(1) + " User");
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Add item decoration for better spacing
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.recycler_item_spacing);
        recyclerView.addItemDecoration(new SpacingItemDecoration(spacingInPixels));
    }

    private void setupFloatingActionButton() {
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
            intent.putExtra("mode", "add");
            startActivityForResult(intent, REQUEST_ADD);
        });
    }

    private void loadEmployeeData() {
        List<Employee> employees = repository.getAllEmployees();

        if (employees.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            if (adapter == null) {
                adapter = new EmployeeAdapter(this, employees);
                setupAdapterClickListeners();
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(employees);
            }
        }

        // Update statistics
        updateStatistics(employees.size());
    }

    private void setupAdapterClickListeners() {
        adapter.setOnItemClickListener(new EmployeeAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Employee employee, int position) {
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                intent.putExtra("mode", "edit");
                intent.putExtra("mastCode", employee.getMastCode());
                startActivityForResult(intent, REQUEST_EDIT);
            }

            @Override
            public void onDeleteClick(Employee employee, int position) {
                showDeleteConfirmation(employee);
            }
        });
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void updateStatistics(int employeeCount) {
        tvTotalEmployees.setText(String.valueOf(employeeCount));
    }

    private void updateLastRefreshTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        tvLastUpdated.setText("Updated " + sdf.format(new Date()));
    }

    private void showDeleteConfirmation(Employee employee) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Employee")
                .setMessage("Are you sure you want to delete " + employee.getEmpName() + "?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteEmployee(employee.getMastCode());
                    refreshEmployeeList();
                    showSuccessMessage("Employee deleted successfully");
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    private void refreshEmployeeList() {
        loadEmployeeData();
        updateLastRefreshTime();
    }

    private void showSuccessMessage(String message) {
        // You can implement a Snackbar here for better UX
        // For now, keeping it simple
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            showLogoutConfirmation();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshEmployeeList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_lock_power_off)
                .show();
    }


    private void performLogout() {
        repository.close();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == REQUEST_ADD || requestCode == REQUEST_EDIT) && resultCode == RESULT_OK) {
            refreshEmployeeList();

            // Show success message based on action
            String message = (requestCode == REQUEST_ADD) ?
                    "Employee added successfully" : "Employee updated successfully";
            showSuccessMessage(message);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshEmployeeList();
    }

    @Override
    protected void onDestroy() {
        if (repository != null) {
            repository.close();
        }
        super.onDestroy();
    }

    // Inner class for RecyclerView item spacing
    private static class SpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public SpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, android.view.View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.bottom = spacing;
        }
    }
}
