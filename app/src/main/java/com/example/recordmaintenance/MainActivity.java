package com.example.recordmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ADD = 100;
    private static final int REQUEST_EDIT = 101;

    private EmployeeRepository repository;
    private EmployeeAdapter adapter;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        repository = new EmployeeRepository(this);
        repository.open();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EmployeeAdapter(this, repository.getAllEmployees());
        recyclerView.setAdapter(adapter);

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
                confirmDelete(employee.getMastCode());
            }
        });

        fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                intent.putExtra("mode", "add");
                startActivityForResult(intent, REQUEST_ADD);
            }
        });
    }

    private void confirmDelete(final int mastCode) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete this employee?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    repository.deleteEmployee(mastCode);
                    refreshList();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void refreshList() {
        List<Employee> employees = repository.getAllEmployees();
        adapter.updateList(employees);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_ADD || requestCode == REQUEST_EDIT) && resultCode == RESULT_OK) {
            refreshList();
        }
    }

    @Override
    protected void onDestroy() {
        repository.close();
        super.onDestroy();
    }
}
