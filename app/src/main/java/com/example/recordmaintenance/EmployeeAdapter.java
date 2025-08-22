package com.example.recordmaintenance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> implements Filterable {

    private Context context;
    private List<Employee> employeeListFull; // Original unfiltered list
    private List<Employee> employeeListFiltered; // Filtered list for display
    private OnItemClickListener listener;
    private FilterCriteria currentFilterCriteria;

    public interface OnItemClickListener {
        void onEditClick(Employee employee, int position);
        void onDeleteClick(Employee employee, int position);
        void onViewClick(Employee employee, int position);
    }

    public static class FilterCriteria {
        public String searchQuery = "";
        public String departmentFilter = "";
        public String designationFilter = "";
        public double minSalary = 0;
        public double maxSalary = Double.MAX_VALUE;
        public String dateRange = ""; // "last_month", "last_year", etc.

        public boolean isEmpty() {
            return searchQuery.isEmpty() && departmentFilter.isEmpty() &&
                    designationFilter.isEmpty() && minSalary == 0 &&
                    maxSalary == Double.MAX_VALUE && dateRange.isEmpty();
        }
    }

    public enum SortCriteria {
        NAME_ASC, NAME_DESC,
        SALARY_ASC, SALARY_DESC,
        JOINED_DATE_ASC, JOINED_DATE_DESC,
        DESIGNATION_ASC, DESIGNATION_DESC,
        DEPARTMENT_ASC, DEPARTMENT_DESC
    }

    public EmployeeAdapter(Context context, List<Employee> employeeList) {
        this.context = context;
        this.employeeListFull = new ArrayList<>(employeeList);
        this.employeeListFiltered = new ArrayList<>(employeeList);
        this.currentFilterCriteria = new FilterCriteria();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Employee employee = employeeListFiltered.get(position);

        holder.tvEmpName.setText(employee.getEmpName());
        holder.tvEmpId.setText(employee.getEmpId() != null ? employee.getEmpId() : "N/A");
        holder.tvDesignation.setText(employee.getDesignation() != null ? employee.getDesignation() : "N/A");
        holder.tvDepartment.setText(employee.getDepartment() != null ? employee.getDepartment() : "N/A");
        holder.tvSalary.setText("₹" + String.format("%.0f", employee.getSalary()));
        holder.tvCity.setText(employee.getCity() != null ? employee.getCity() : "N/A");
        holder.tvJoinedDate.setText(employee.getJoinedDate() != null ? employee.getJoinedDate() : "N/A");
        holder.tvEmail.setText(employee.getEmpEmail() != null ? employee.getEmpEmail() : "N/A");

        // Set click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(employee, position);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(employee, position);
            }
        });

        holder.btnView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewClick(employee, position);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewClick(employee, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return employeeListFiltered.size();
    }

    public void updateList(List<Employee> newList) {
        this.employeeListFull = new ArrayList<>(newList);
        this.employeeListFiltered = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    // Sorting Methods
    public void sortBy(SortCriteria criteria) {
        switch (criteria) {
            case NAME_ASC:
                Collections.sort(employeeListFiltered, (e1, e2) ->
                        e1.getEmpName().compareToIgnoreCase(e2.getEmpName()));
                break;
            case NAME_DESC:
                Collections.sort(employeeListFiltered, (e1, e2) ->
                        e2.getEmpName().compareToIgnoreCase(e1.getEmpName()));
                break;
            case SALARY_ASC:
                Collections.sort(employeeListFiltered, Comparator.comparing(Employee::getSalary));
                break;
            case SALARY_DESC:
                Collections.sort(employeeListFiltered, (e1, e2) ->
                        Double.compare(e2.getSalary(), e1.getSalary()));
                break;
            case JOINED_DATE_ASC:
                Collections.sort(employeeListFiltered, (e1, e2) -> {
                    if (e1.getJoinedDate() == null) return 1;
                    if (e2.getJoinedDate() == null) return -1;
                    return e1.getJoinedDate().compareTo(e2.getJoinedDate());
                });
                break;
            case JOINED_DATE_DESC:
                Collections.sort(employeeListFiltered, (e1, e2) -> {
                    if (e1.getJoinedDate() == null) return 1;
                    if (e2.getJoinedDate() == null) return -1;
                    return e2.getJoinedDate().compareTo(e1.getJoinedDate());
                });
                break;
            case DESIGNATION_ASC:
                Collections.sort(employeeListFiltered, (e1, e2) -> {
                    String d1 = e1.getDesignation() != null ? e1.getDesignation() : "";
                    String d2 = e2.getDesignation() != null ? e2.getDesignation() : "";
                    return d1.compareToIgnoreCase(d2);
                });
                break;
            case DESIGNATION_DESC:
                Collections.sort(employeeListFiltered, (e1, e2) -> {
                    String d1 = e1.getDesignation() != null ? e1.getDesignation() : "";
                    String d2 = e2.getDesignation() != null ? e2.getDesignation() : "";
                    return d2.compareToIgnoreCase(d1);
                });
                break;
            case DEPARTMENT_ASC:
                Collections.sort(employeeListFiltered, (e1, e2) -> {
                    String d1 = e1.getDepartment() != null ? e1.getDepartment() : "";
                    String d2 = e2.getDepartment() != null ? e2.getDepartment() : "";
                    return d1.compareToIgnoreCase(d2);
                });
                break;
            case DEPARTMENT_DESC:
                Collections.sort(employeeListFiltered, (e1, e2) -> {
                    String d1 = e1.getDepartment() != null ? e1.getDepartment() : "";
                    String d2 = e2.getDepartment() != null ? e2.getDepartment() : "";
                    return d2.compareToIgnoreCase(d1);
                });
                break;
        }
        notifyDataSetChanged();
    }

    // Advanced filtering with multiple criteria
    public void applyAdvancedFilter(FilterCriteria criteria) {
        this.currentFilterCriteria = criteria;
        getFilter().filter("");
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Employee> filteredList = new ArrayList<>();

                if (currentFilterCriteria.isEmpty() && constraint.toString().isEmpty()) {
                    filteredList.addAll(employeeListFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (Employee employee : employeeListFull) {
                        boolean matchesSearch = true;
                        boolean matchesDepartment = true;
                        boolean matchesDesignation = true;
                        boolean matchesSalary = true;

                        // Text search (name, email, ID)
                        if (!filterPattern.isEmpty()) {
                            String name = employee.getEmpName() != null ? employee.getEmpName().toLowerCase() : "";
                            String email = employee.getEmpEmail() != null ? employee.getEmpEmail().toLowerCase() : "";
                            String empId = employee.getEmpId() != null ? employee.getEmpId().toLowerCase() : "";

                            matchesSearch = name.contains(filterPattern) ||
                                    email.contains(filterPattern) ||
                                    empId.contains(filterPattern);
                        }

                        // Advanced search from search query
                        if (!currentFilterCriteria.searchQuery.isEmpty()) {
                            String query = currentFilterCriteria.searchQuery.toLowerCase();
                            String name = employee.getEmpName() != null ? employee.getEmpName().toLowerCase() : "";
                            String email = employee.getEmpEmail() != null ? employee.getEmpEmail().toLowerCase() : "";
                            String empId = employee.getEmpId() != null ? employee.getEmpId().toLowerCase() : "";
                            String designation = employee.getDesignation() != null ? employee.getDesignation().toLowerCase() : "";
                            String department = employee.getDepartment() != null ? employee.getDepartment().toLowerCase() : "";

                            matchesSearch = name.contains(query) || email.contains(query) ||
                                    empId.contains(query) || designation.contains(query) ||
                                    department.contains(query);
                        }

                        // Department filter
                        if (!currentFilterCriteria.departmentFilter.isEmpty()) {
                            String empDept = employee.getDepartment() != null ? employee.getDepartment() : "";
                            matchesDepartment = empDept.equals(currentFilterCriteria.departmentFilter);
                        }

                        // Designation filter
                        if (!currentFilterCriteria.designationFilter.isEmpty()) {
                            String empDesig = employee.getDesignation() != null ? employee.getDesignation() : "";
                            matchesDesignation = empDesig.equals(currentFilterCriteria.designationFilter);
                        }

                        // Salary range filter
                        double salary = employee.getSalary();
                        matchesSalary = salary >= currentFilterCriteria.minSalary &&
                                salary <= currentFilterCriteria.maxSalary;

                        // Combine all filters
                        if (matchesSearch && matchesDepartment && matchesDesignation && matchesSalary) {
                            filteredList.add(employee);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                employeeListFiltered.clear();
                employeeListFiltered.addAll((List<Employee>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    public int getFilteredCount() {
        return employeeListFiltered.size();
    }

    public int getTotalCount() {
        return employeeListFull.size();
    }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmpName, tvEmpId, tvDesignation, tvDepartment, tvSalary, tvCity, tvJoinedDate, tvEmail;
        ImageButton btnEdit, btnDelete, btnView;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmpName = itemView.findViewById(R.id.tvEmpName);
            tvEmpId = itemView.findViewById(R.id.tvEmpId);
            tvDesignation = itemView.findViewById(R.id.tvDesignation);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvSalary = itemView.findViewById(R.id.tvSalary);
            tvCity = itemView.findViewById(R.id.tvCity);
            tvJoinedDate = itemView.findViewById(R.id.tvJoinedDate);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnView = itemView.findViewById(R.id.btnView);
        }
    }
}
