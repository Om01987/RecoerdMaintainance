package com.example.recordmaintenance;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Arrays;
import java.util.List;

public class FilterSortDialogFragment extends DialogFragment {

    private TextInputEditText etSearchQuery;
    private AutoCompleteTextView spinnerDepartment, spinnerDesignation, spinnerSortBy;
    private RangeSlider salaryRangeSlider;
    private ChipGroup chipGroupQuickFilters;
    private MaterialButton btnApplyFilter, btnClearAll;

    private FilterSortListener listener;
    private EmployeeAdapter.FilterCriteria currentCriteria;

    public interface FilterSortListener {
        void onFilterApplied(EmployeeAdapter.FilterCriteria criteria, EmployeeAdapter.SortCriteria sortCriteria);
        void onClearAllFilters();
        List<String> getDepartmentList();
        List<String> getDesignationList();
    }

    public static FilterSortDialogFragment newInstance(EmployeeAdapter.FilterCriteria currentCriteria) {
        FilterSortDialogFragment fragment = new FilterSortDialogFragment();
        fragment.currentCriteria = currentCriteria != null ? currentCriteria : new EmployeeAdapter.FilterCriteria();
        return fragment;
    }

    public void setFilterSortListener(FilterSortListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter_sort, null);

        initializeViews(view);
        setupDropdowns();
        setupSlider();
        setupQuickFilters();
        setupButtons();
        loadCurrentCriteria();

        return new AlertDialog.Builder(requireContext())
                .setTitle("🔍 Filter & Sort Employees")
                .setView(view)
                .create();
    }

    private void initializeViews(View view) {
        etSearchQuery = view.findViewById(R.id.etSearchQuery);
        spinnerDepartment = view.findViewById(R.id.spinnerDepartment);
        spinnerDesignation = view.findViewById(R.id.spinnerDesignation);
        spinnerSortBy = view.findViewById(R.id.spinnerSortBy);
        salaryRangeSlider = view.findViewById(R.id.salaryRangeSlider);
        chipGroupQuickFilters = view.findViewById(R.id.chipGroupQuickFilters);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        btnClearAll = view.findViewById(R.id.btnClearAll);
    }

    private void setupDropdowns() {
        // Department dropdown
        if (listener != null) {
            List<String> departments = listener.getDepartmentList();
            departments.add(0, "All Departments");
            ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_dropdown_item_1line, departments);
            spinnerDepartment.setAdapter(deptAdapter);

            List<String> designations = listener.getDesignationList();
            designations.add(0, "All Designations");
            ArrayAdapter<String> desigAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_dropdown_item_1line, designations);
            spinnerDesignation.setAdapter(desigAdapter);
        }

        // Sort options
        String[] sortOptions = {
                "Name (A-Z)", "Name (Z-A)",
                "Salary (Low to High)", "Salary (High to Low)",
                "Joined Date (Newest)", "Joined Date (Oldest)",
                "Designation (A-Z)", "Designation (Z-A)",
                "Department (A-Z)", "Department (Z-A)"
        };
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, Arrays.asList(sortOptions));
        spinnerSortBy.setAdapter(sortAdapter);
    }

    private void setupSlider() {
        salaryRangeSlider.setValueFrom(0f);
        salaryRangeSlider.setValueTo(200000f);
        salaryRangeSlider.setStepSize(5000f);
        salaryRangeSlider.setValues(0f, 200000f);
    }

    private void setupQuickFilters() {
        String[] quickFilters = {
                "Engineering", "Software Development", "HR", "Finance",
                "New Joiners (Last 3 months)", "High Salary (>₹50K)", "Remote Workers"
        };

        for (String filter : quickFilters) {
            Chip chip = new Chip(getContext());
            chip.setText(filter);
            chip.setCheckable(true);
            chipGroupQuickFilters.addView(chip);
        }
    }

    private void setupButtons() {
        btnApplyFilter.setOnClickListener(v -> applyFilters());
        btnClearAll.setOnClickListener(v -> clearAllFilters());
    }

    private void loadCurrentCriteria() {
        if (currentCriteria != null) {
            etSearchQuery.setText(currentCriteria.searchQuery);
            spinnerDepartment.setText(currentCriteria.departmentFilter);
            spinnerDesignation.setText(currentCriteria.designationFilter);

            if (currentCriteria.minSalary > 0 || currentCriteria.maxSalary < Double.MAX_VALUE) {
                salaryRangeSlider.setValues(
                        (float) currentCriteria.minSalary,
                        currentCriteria.maxSalary == Double.MAX_VALUE ? 200000f : (float) currentCriteria.maxSalary
                );
            }
        }
    }

    private void applyFilters() {
        EmployeeAdapter.FilterCriteria criteria = new EmployeeAdapter.FilterCriteria();

        // Search query
        criteria.searchQuery = etSearchQuery.getText().toString().trim();

        // Department filter
        String selectedDept = spinnerDepartment.getText().toString();
        if (!selectedDept.equals("All Departments") && !selectedDept.isEmpty()) {
            criteria.departmentFilter = selectedDept;
        }

        // Designation filter
        String selectedDesig = spinnerDesignation.getText().toString();
        if (!selectedDesig.equals("All Designations") && !selectedDesig.isEmpty()) {
            criteria.designationFilter = selectedDesig;
        }

        // Salary range
        List<Float> salaryValues = salaryRangeSlider.getValues();
        criteria.minSalary = salaryValues.get(0);
        criteria.maxSalary = salaryValues.get(1);

        // Sort criteria
        EmployeeAdapter.SortCriteria sortCriteria = getSortCriteriaFromSelection();

        if (listener != null) {
            listener.onFilterApplied(criteria, sortCriteria);
        }

        dismiss();
    }

    private EmployeeAdapter.SortCriteria getSortCriteriaFromSelection() {
        String selection = spinnerSortBy.getText().toString();
        switch (selection) {
            case "Name (A-Z)": return EmployeeAdapter.SortCriteria.NAME_ASC;
            case "Name (Z-A)": return EmployeeAdapter.SortCriteria.NAME_DESC;
            case "Salary (Low to High)": return EmployeeAdapter.SortCriteria.SALARY_ASC;
            case "Salary (High to Low)": return EmployeeAdapter.SortCriteria.SALARY_DESC;
            case "Joined Date (Newest)": return EmployeeAdapter.SortCriteria.JOINED_DATE_DESC;
            case "Joined Date (Oldest)": return EmployeeAdapter.SortCriteria.JOINED_DATE_ASC;
            case "Designation (A-Z)": return EmployeeAdapter.SortCriteria.DESIGNATION_ASC;
            case "Designation (Z-A)": return EmployeeAdapter.SortCriteria.DESIGNATION_DESC;
            case "Department (A-Z)": return EmployeeAdapter.SortCriteria.DEPARTMENT_ASC;
            case "Department (Z-A)": return EmployeeAdapter.SortCriteria.DEPARTMENT_DESC;
            default: return EmployeeAdapter.SortCriteria.NAME_ASC;
        }
    }

    private void clearAllFilters() {
        if (listener != null) {
            listener.onClearAllFilters();
        }
        dismiss();
    }
}
