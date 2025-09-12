package com.example.recordmaintenance;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintJob;
import android.print.PrintManager;
import android.util.Log;
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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ADD = 100;
    private static final int REQUEST_EDIT = 101;
    private static final int REQ_CREATE_CSV = 1020;

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
    private AuthRepository authRepository;
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

        // Initialize repositories
        repository = new EmployeeRepository(this);
        authRepository = new AuthRepository(this);
        repository.open();

        // Get current role from intent (fallback to admin)
        currentRole = getIntent().getStringExtra("role");
        currentEmployeeId = getIntent().getStringExtra("employeeId");
        if (currentRole == null) currentRole = "admin";

        // Initialize UI
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFloatingActionButton();
        setupQuickAdd();
        setupSearchAndFilter();

        // Employee redirect (should not happen with new auth flow, but keeping as safety)
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
        // Show loading state
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        // Load employees from Firebase
        repository.getAllEmployees(new EmployeeRepository.EmployeeListCallback() {
            @Override
            public void onSuccess(List<Employee> employees) {
                // Update UI on main thread
                runOnUiThread(() -> {
                    displayEmployees(employees);
                });
            }

            @Override
            public void onError(String error) {
                // Handle error on main thread
                runOnUiThread(() -> {
                    Log.e(TAG, "Failed to load employees: " + error);
                    Toast.makeText(MainActivity.this, "Failed to load employees: " + error, Toast.LENGTH_LONG).show();

                    // Show empty state
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
            }
        });
    }

    private void displayEmployees(List<Employee> employees) {
        if (employees.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (adapter == null) {
                adapter = new EmployeeAdapter(this, employees);
                setupAdapterClickListeners();
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(employees);
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
                i.putExtra("employeeUid", e.getUid()); // Use Firebase UID instead of mastCode
                startActivityForResult(i,REQUEST_EDIT);
            }

            @Override
            public void onDeleteClick(Employee e, int pos) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete "+e.getEmpName()+"?")
                        .setMessage("Are you sure you want to delete this employee?\n\nThis action cannot be undone.")
                        .setPositiveButton("Delete",(d,w)-> {
                            deleteEmployee(e);
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

    private void deleteEmployee(Employee employee) {
        repository.deleteEmployee(employee.getUid(), new EmployeeRepository.DeleteCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    loadEmployeeData(); // Refresh the list
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed to delete employee: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showEmployeeDetailsDialog(Employee employee) {
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

        details.append("🔐 Account Status\n\n");
        details.append("Employee ID: ").append(employee.getEmpId()).append("\n");
        details.append("Password Status: ").append(employee.isPasswordChanged() ? "Changed by employee" : "Initial password");

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Employee Details")
                .setMessage(details.toString())
                .setPositiveButton("Close", null);

        // Note: Password viewing/reset functionality would need to be implemented
        // with Firebase Admin SDK or Cloud Functions for security
        builder.show();
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
                if (adapter != null) {
                    List<Employee> employees = adapter.getCurrentItems();
                    for (Employee emp : employees) {
                        if (emp.getDepartment() != null && !emp.getDepartment().trim().isEmpty()) {
                            departments.add(emp.getDepartment());
                        }
                    }
                }
                return new ArrayList<>(departments);
            }

            @Override
            public List<String> getDesignationList() {
                Set<String> designations = new HashSet<>();
                if (adapter != null) {
                    List<Employee> employees = adapter.getCurrentItems();
                    for (Employee emp : employees) {
                        if (emp.getDesignation() != null && !emp.getDesignation().trim().isEmpty()) {
                            designations.add(emp.getDesignation());
                        }
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

            tvTotalEmployees.setText(String.valueOf(filtered));
        }
    }

    private void updateLastRefreshTime() {
        String t = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date());
        tvLastUpdated.setText("Updated "+t);
    }

    // ============= CSV EXPORT FUNCTIONALITY =============
    private void launchCreateCsvDocument() {
        String suggestedName = "Employees_" +
                new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(new Date()) +
                ".csv";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);

        try {
            startActivityForResult(intent, REQ_CREATE_CSV);
        } catch (Exception e) {
            Toast.makeText(this, "File picker not available: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<Employee> getEmployeesForExport() {
        if (adapter != null) {
            try {
                return adapter.getCurrentItems();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    private void exportToUri(Uri uri) {
        OutputStream outputStream = null;
        try {
            outputStream = getContentResolver().openOutputStream(uri, "w");
            if (outputStream == null) {
                Toast.makeText(this, "Cannot open file for writing", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Employee> employeesToExport = getEmployeesForExport();
            CsvUtils.writeEmployeesToCsv(outputStream, employeesToExport);

            String message = "Successfully exported " + employeesToExport.size() + " employee(s) to CSV";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignore) {}
            }
        }
    }

    // ============= PDF EXPORT FUNCTIONALITY =============
    private void exportListAsPdf() {
        List<Employee> employeesToExport = getEmployeesForExport();

        if (employeesToExport.isEmpty()) {
            Toast.makeText(this, "No employees to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            String jobName = "Employee List - " +
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            ListPrintAdapter printAdapter = new ListPrintAdapter(this, employeesToExport, jobName);
            PrintAttributes.Builder builder = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(new PrintAttributes.Resolution("pdf", "PDF", 600, 600))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS);

            PrintJob printJob = printManager.print(jobName, printAdapter, builder.build());

            if (printJob != null) {
                Toast.makeText(this, "Select 'Save as PDF' in the print dialog to save to your chosen location",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to create print job", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error in PDF export", e);
        }
    }

    // ============= MENU HANDLING =============
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

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
            return true;
        }

        if (id == R.id.action_filter_sort) {
            showFilterSortDialog();
            return true;
        }

        if (id == R.id.action_export_csv) {
            if (adapter == null || adapter.getFilteredCount() == 0) {
                Toast.makeText(this, "No employees to export", Toast.LENGTH_SHORT).show();
            } else {
                launchCreateCsvDocument();
            }
            return true;
        }

        if (id == R.id.action_export_pdf) {
            if (adapter == null || adapter.getFilteredCount() == 0) {
                Toast.makeText(this, "No employees to export", Toast.LENGTH_SHORT).show();
            } else {
                exportListAsPdf();
            }
            return true;
        }

        if (id == R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout",(d,w)-> {
                        authRepository.signOut();
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
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);

        if ((req == REQUEST_ADD || req == REQUEST_EDIT) && res == RESULT_OK) {
            loadEmployeeData();
            updateLastRefreshTime();
            String message = (req == REQUEST_ADD) ? "Employee added successfully" : "Employee updated successfully";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return;
        }

        if (req == REQ_CREATE_CSV && res == RESULT_OK && data != null && data.getData() != null) {
            exportToUri(data.getData());
            return;
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
        if (authRepository != null) {
            authRepository.close();
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