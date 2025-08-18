package com.example.recordmaintenance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private Context context;
    private List<Employee> employeeList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditClick(Employee employee, int position);
        void onDeleteClick(Employee employee, int position);
    }

    public EmployeeAdapter(Context context, List<Employee> employeeList) {
        this.context = context;
        this.employeeList = employeeList;
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
        Employee employee = employeeList.get(position);

        holder.tvEmpName.setText(employee.getEmpName());
        holder.tvDesignation.setText(employee.getDesignation());
        holder.tvDepartment.setText(employee.getDepartment());
        holder.tvSalary.setText("₹" + String.valueOf(employee.getSalary()));
        holder.tvCity.setText(employee.getCity() != null ? employee.getCity() : "N/A");
        holder.tvJoinedDate.setText(employee.getJoinedDate());

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
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public void updateList(List<Employee> newList) {
        this.employeeList = newList;
        notifyDataSetChanged();
    }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmpName, tvDesignation, tvDepartment, tvSalary, tvCity, tvJoinedDate;
        ImageButton btnEdit, btnDelete;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmpName = itemView.findViewById(R.id.tvEmpName);
            tvDesignation = itemView.findViewById(R.id.tvDesignation);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvSalary = itemView.findViewById(R.id.tvSalary);
            tvCity = itemView.findViewById(R.id.tvCity);
            tvJoinedDate = itemView.findViewById(R.id.tvJoinedDate);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
