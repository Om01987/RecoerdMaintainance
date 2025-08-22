package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private TextView tvFilterStatus;
    private View emptyStateLayout;
    private CardView cvQuickAdd;

    // Data Components
    private EmployeeRepository repository;
    private EmployeeAdapter adapter;
    private String currentRole;
    private String currentEmployeeId;

    // Filter & Search Components
    private SearchView searchView;
    private EmployeeAdapter.FilterCriteria currentFilterCriteria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize data
        currentRole = getIntent().getStringExtra("role");
        currentEmployeeId = getIntent().getStringExtra("employeeId");
        if (currentRole == null) currentRole = "admin";

        repository = new EmployeeRepository(this);
        repository.open();

        // Initialize UI
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFloatingActionButton();
        setupQuickAdd();
        setupSearchAndFilter();

        // Employee redirect
        if ("employee".equals(currentRole)) {
            redirectEmployeeToProfile();
            return;
        }

        // Load data for admin
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
        tvFilterStatus = findViewById(R.id.tvFilterStatus);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        cvQuickAdd = findViewById(R.id.cvQuickAdd);

        if ("employee".equals(currentRole)) {
            if (fabAdd != null) fabAdd.setVisibility(View.GONE);
            if (cvQuickAdd != null) cvQuickAdd.setVisibility(View.GONE);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                    "employee".equals(currentRole) ? "My Profile" : "Employee Management System"
            );
        }
        tvLoggedInAs.setText(currentRole.substring(0,1).toUpperCase() + currentRole.substring(1) + " User");
    }

    private void redirectEmployeeToProfile() {
        Intent intent = new Intent(this, EmployeeProfileActivity.class);
        intent.putExtra("employeeId", currentEmployeeId);
        startActivity(intent);
        finish();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        int spacing = getResources().getDimensionPixelSize(R.dimen.recycler_item_spacing);
        recyclerView.addItemDecoration(new SpacingItemDecoration(spacing));
    }

    private void setupFloatingActionButton() {
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
            intent.putExtra("mode", "add");
            startActivityForResult(intent, REQUEST_ADD);
        });
    }

    private void setupQuickAdd() {
        if (cvQuickAdd != null) {
            cvQuickAdd.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                intent.putExtra("mode", "add");
                startActivityForResult(intent, REQUEST_ADD);
                Toast.makeText(this, "Opening Quick Add Form", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupSearchAndFilter() {
        currentFilterCriteria = new EmployeeAdapter.FilterCriteria();
    }

    private void loadEmployeeData() {
        List<Employee> list = repository.getAllEmployees();
        if (list.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            if (adapter == null) {
                adapter = new EmployeeAdapter(this, list);
                setupAdapterClickListeners();
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(list);
            }
        }
        updateFilterStatus();
    }

    private void setupAdapterClickListeners() {
        adapter.setOnItemClickListener(new EmployeeAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Employee e, int pos) {
                Intent i = new Intent(MainActivity.this, AddEditActivity.class);
                i.putExtra("mode","edit");
                i.putExtra("mastCode",e.getMastCode());
                startActivityForResult(i,REQUEST_EDIT);
            }

            @Override
            public void onDeleteClick(Employee e, int pos) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete "+e.getEmpName()+"?")
                        .setMessage("Are you sure you want to delete this employee?\n\nThis action cannot be undone.")
                        .setPositiveButton("Delete",(d,w)-> {
                            repository.deleteEmployee(e.getMastCode());
                            loadEmployeeData();
                            Toast.makeText(MainActivity.this, "Employee deleted successfully", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel",null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }

            @Override
            public void onViewClick(Employee e, int pos) {
                showEmployeeDetailsDialog(e);
            }
        });
    }

    private void showEmployeeDetailsDialog(Employee employee) {
        String plainPassword = repository.getEmployeePassword(employee.getEmpId());

        StringBuilder details = new StringBuilder();
        details.append("👤 Employee Details\n\n");
        details.append("ID: ").append(employee.getEmpId() != null ? employee.getEmpId() : "N/A").append("\n");
        details.append("Name: ").append(employee.getEmpName()).append("\n");
        details.append("Email: ").append(employee.getEmpEmail() != null ? employee.getEmpEmail() : "N/A").append("\n");
        details.append("Designation: ").append(employee.getDesignation() != null ? employee.getDesignation() : "N/A").append("\n");
        details.append("Department: ").append(employee.getDepartment() != null ? employee.getDepartment() : "N/A").append("\n");
        details.append("Salary: ₹").append(String.format("%.0f", employee.getSalary())).append("\n");
        details.append("Joined: ").append(employee.getJoinedDate() != null ? employee.getJoinedDate() : "N/A").append("\n\n");

        details.append("📍 Address Details\n\n");
        details.append("Address Line 1: ").append(employee.getAddressLine1() != null ? employee.getAddressLine1() : "N/A").append("\n");
        details.append("Address Line 2: ").append(employee.getAddressLine2() != null ? employee.getAddressLine2() : "N/A").append("\n");
        details.append("City: ").append(employee.getCity() != null ? employee.getCity() : "N/A").append("\n");
        details.append("State: ").append(employee.getState() != null ? employee.getState() : "N/A").append("\n");
        details.append("Country: ").append(employee.getCountry() != null ? employee.getCountry() : "N/A").append("\n\n");

        details.append("🔐 Login Credentials\n\n");
        details.append("Employee ID: ").append(employee.getEmpId()).append("\n");
        details.append("Current Password: ").append(getPasswordDisplayText(plainPassword)).append("\n");
        details.append("Password Status: ").append(employee.isPasswordChanged() ? "Changed by employee" : "Initial password");

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Employee Login Credentials")
                .setMessage(details.toString())
                .setPositiveButton("Close", null);

        // Add appropriate action buttons based on password status
        if ("PASSWORD_CHANGED_BY_EMPLOYEE".equals(plainPassword)) {
            // Password was changed - offer reset option
            builder.setNegativeButton("Reset Password", (dialog, which) -> {
                showResetPasswordConfirmation(employee);
            });
        } else if (isPasswordCopyable(plainPassword)) {
            // Password is viewable - offer copy option
            builder.setNeutralButton("Copy Password", (dialog, which) -> {
                copyPasswordToClipboard(plainPassword);
            });
        }

        builder.show();
    }

    private String getPasswordDisplayText(String plainPassword) {
        if (plainPassword == null || "EMPLOYEE_NOT_FOUND".equals(plainPassword)) {
            return "Not available";
        } else if ("PASSWORD_CHANGED_BY_EMPLOYEE".equals(plainPassword)) {
            return "Changed by employee (not viewable)";
        } else {
            return plainPassword;
        }
    }

    private boolean isPasswordCopyable(String plainPassword) {
        return plainPassword != null &&
                !plainPassword.equals("EMPLOYEE_NOT_FOUND") &&
                !plainPassword.equals("PASSWORD_CHANGED_BY_EMPLOYEE") &&
                !plainPassword.contains("not viewable") &&
                !plainPassword.contains("Not available");
    }

    private void copyPasswordToClipboard(String password) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Employee Password", password);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void showResetPasswordConfirmation(Employee employee) {
        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Reset password for " + employee.getEmpName() + "?\n\nThis will generate a new initial password that you can share with the employee. The employee will be able to change it after logging in.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    String newPassword = repository.resetEmployeePassword(employee.getEmpId());
                    if (newPassword != null) {
                        showNewPasswordDialog(employee, newPassword);
                    } else {
                        Toast.makeText(this, "Failed to reset password", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_lock_power_off)
                .show();
    }

    private void showNewPasswordDialog(Employee employee, String newPassword) {
        String message = "✅ Password Reset Successfully!\n\n" +
                "Employee: " + employee.getEmpName() + "\n" +
                "Employee ID: " + employee.getEmpId() + "\n" +
                "New Password: " + newPassword + "\n\n" +
                "⚠️ Please share these credentials with the employee securely.\n" +
                "The employee can change this password after logging in.";

        new AlertDialog.Builder(this)
                .setTitle("New Password Generated")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Refresh the employee list to reflect changes
                    loadEmployeeData();
                })
                .setNeutralButton("Copy Password", (dialog, which) -> {
                    copyPasswordToClipboard(newPassword);
                    // Refresh the employee list to reflect changes
                    loadEmployeeData();
                })
                .setCancelable(false)
                .show();
    }

    private void showFilterSortDialog() {
        FilterSortDialogFragment dialog = FilterSortDialogFragment.newInstance(currentFilterCriteria);
        dialog.setFilterSortListener(new FilterSortDialogFragment.FilterSortListener() {
            @Override
            public void onFilterApplied(EmployeeAdapter.FilterCriteria criteria, EmployeeAdapter.SortCriteria sortCriteria) {
                currentFilterCriteria = criteria;
                if (adapter != null) {
                    adapter.applyAdvancedFilter(criteria);
                    adapter.sortBy(sortCriteria);
                    updateFilterStatus();
                }
                Toast.makeText(MainActivity.this, "Filter and sort applied", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onClearAllFilters() {
                currentFilterCriteria = new EmployeeAdapter.FilterCriteria();
                if (adapter != null) {
                    adapter.applyAdvancedFilter(currentFilterCriteria);
                    updateFilterStatus();
                }
                if (searchView != null) {
                    searchView.setQuery("", false);
                    searchView.clearFocus();
                }
                Toast.makeText(MainActivity.this, "All filters cleared", Toast.LENGTH_SHORT).show();
            }

            @Override
            public List<String> getDepartmentList() {
                Set<String> departments = new HashSet<>();
                for (Employee emp : repository.getAllEmployees()) {
                    if (emp.getDepartment() != null && !emp.getDepartment().trim().isEmpty()) {
                        departments.add(emp.getDepartment());
                    }
                }
                return new ArrayList<>(departments);
            }

            @Override
            public List<String> getDesignationList() {
                Set<String> designations = new HashSet<>();
                for (Employee emp : repository.getAllEmployees()) {
                    if (emp.getDesignation() != null && !emp.getDesignation().trim().isEmpty()) {
                        designations.add(emp.getDesignation());
                    }
                }
                return new ArrayList<>(designations);
            }
        });

        dialog.show(getSupportFragmentManager(), "FilterSortDialog");
    }

    private void updateFilterStatus() {
        if (adapter != null) {
            int filtered = adapter.getFilteredCount();
            int total = adapter.getTotalCount();

            if (tvFilterStatus != null) {
                if (filtered < total) {
                    tvFilterStatus.setVisibility(View.VISIBLE);
                    tvFilterStatus.setText("Showing " + filtered + " of " + total + " employees");
                } else {
                    tvFilterStatus.setVisibility(View.GONE);
                }
            }

            // Update total employees count
            tvTotalEmployees.setText(String.valueOf(filtered));
        }
    }

    private void updateLastRefreshTime() {
        String t = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date());
        tvLastUpdated.setText("Updated "+t);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Setup search view
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Search employees...");
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (adapter != null) {
                            adapter.getFilter().filter(newText);
                            updateFilterStatus();
                        }
                        return true;
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            loadEmployeeData();
            updateLastRefreshTime();
            Toast.makeText(this, "Data refreshed", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.action_search) {
            // Search functionality is handled in onCreateOptionsMenu
            return true;
        }

        if (id == R.id.action_filter_sort) {
            showFilterSortDialog();
            return true;
        }

        if (id == R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout",(d,w)-> {
                        repository.close();
                        startActivity(new Intent(this,LoginActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK));
                        finish();
                    })
                    .setNegativeButton("Cancel",null)
                    .setIcon(android.R.drawable.ic_lock_power_off)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent d) {
        super.onActivityResult(req, res, d);
        if ((req == REQUEST_ADD || req == REQUEST_EDIT) && res == RESULT_OK) {
            loadEmployeeData();
            updateLastRefreshTime();
            String message = (req == REQUEST_ADD) ? "Employee added successfully" : "Employee updated successfully";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!"employee".equals(currentRole)) {
            loadEmployeeData();
        }
    }

    @Override
    protected void onDestroy() {
        if (repository != null) {
            repository.close();
        }
        super.onDestroy();
    }

    private static class SpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        SpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect o, View v,
                                   RecyclerView p, RecyclerView.State s) {
            o.bottom = spacing;
        }
    }
}
