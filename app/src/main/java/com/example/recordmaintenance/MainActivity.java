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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;
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
    private CardView cvQuickAdd;

    // Data Components
    private EmployeeRepository repository;
    private EmployeeAdapter adapter;
    private String currentRole;
    private String currentEmployeeId;

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
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        cvQuickAdd = findViewById(R.id.cvQuickAdd);

        if ("employee".equals(currentRole)) {
            fabAdd.setVisibility(View.GONE);
            cvQuickAdd.setVisibility(View.GONE);
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
        cvQuickAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
            intent.putExtra("mode", "add");
            startActivityForResult(intent, REQUEST_ADD);
            Toast.makeText(this, "Opening Quick Add Form", Toast.LENGTH_SHORT).show();
        });
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
        tvTotalEmployees.setText(String.valueOf(list.size()));
    }

    private void setupAdapterClickListeners() {
        adapter.setOnItemClickListener(new EmployeeAdapter.OnItemClickListener() {
            @Override public void onEditClick(Employee e, int pos) {
                Intent i = new Intent(MainActivity.this, AddEditActivity.class);
                i.putExtra("mode","edit"); i.putExtra("mastCode",e.getMastCode());
                startActivityForResult(i,REQUEST_EDIT);
            }
            @Override public void onDeleteClick(Employee e, int pos) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete "+e.getEmpName()+"?")
                        .setMessage("Confirm deletion?")
                        .setPositiveButton("Delete",(d,w)-> {
                            repository.deleteEmployee(e.getMastCode());
                            loadEmployeeData();
                        })
                        .setNegativeButton("Cancel",null)
                        .show();
            }
            @Override public void onViewClick(Employee e, int pos) {
                String pwd = repository.getEmployeePassword(e.getEmpId());
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(e.getEmpName()+" Credentials")
                        .setMessage("ID: "+e.getEmpId()+"\nPassword: "+pwd)
                        .setPositiveButton("OK",null)
                        .show();
            }
        });
    }

    private void updateLastRefreshTime() {
        String t = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date());
        tvLastUpdated.setText("Updated "+t);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.main_menu,m);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.action_refresh) { loadEmployeeData(); return true; }
        if(id==R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Logout?")
                    .setPositiveButton("Yes",(d,w)->{
                        repository.close();
                        startActivity(new Intent(this,LoginActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK));
                        finish();
                    })
                    .setNegativeButton("No",null).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override protected void onActivityResult(int req,int res,@Nullable Intent d){
        super.onActivityResult(req,res,d);
        if((req==REQUEST_ADD||req==REQUEST_EDIT)&&res==RESULT_OK){
            loadEmployeeData();
            Toast.makeText(this, req==REQUEST_ADD?"Added":"Updated", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onDestroy(){
        repository.close();
        super.onDestroy();
    }

    private static class SpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;
        SpacingItemDecoration(int spacing){ this.spacing=spacing; }
        @Override public void getItemOffsets(android.graphics.Rect o, View v,
                                             RecyclerView p, RecyclerView.State s){
            o.bottom=spacing;
        }
    }
}
