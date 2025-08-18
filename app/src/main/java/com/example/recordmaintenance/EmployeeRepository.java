package com.example.recordmaintenance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRepository {

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public EmployeeRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // Insert Employee (Master + Detail in transaction)
    public long insertEmployee(Employee employee) {
        database.beginTransaction();
        try {
            // Insert Master
            ContentValues masterValues = new ContentValues();
            masterValues.put(DatabaseHelper.EMP_ID, employee.getEmpId());
            masterValues.put(DatabaseHelper.EMP_NAME, employee.getEmpName());
            masterValues.put(DatabaseHelper.DESIGNATION, employee.getDesignation());
            masterValues.put(DatabaseHelper.DEPARTMENT, employee.getDepartment());
            masterValues.put(DatabaseHelper.JOINED_DATE, employee.getJoinedDate());
            masterValues.put(DatabaseHelper.SALARY, employee.getSalary());

            long mastCode = database.insert(DatabaseHelper.TABLE_MASTER, null, masterValues);

            if (mastCode != -1) {
                // Insert Detail
                ContentValues detailValues = new ContentValues();
                detailValues.put(DatabaseHelper.EMP_CODE, mastCode);
                detailValues.put(DatabaseHelper.ADDRESS_LINE1, employee.getAddressLine1());
                detailValues.put(DatabaseHelper.ADDRESS_LINE2, employee.getAddressLine2());
                detailValues.put(DatabaseHelper.CITY, employee.getCity());
                detailValues.put(DatabaseHelper.STATE, employee.getState());
                detailValues.put(DatabaseHelper.COUNTRY, employee.getCountry());

                long detailResult = database.insert(DatabaseHelper.TABLE_DETAIL, null, detailValues);

                if (detailResult != -1) {
                    database.setTransactionSuccessful();
                    return mastCode;
                }
            }
            return -1;
        } finally {
            database.endTransaction();
        }
    }

    // Get All Employees with Details (JOIN query)
    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();

        String query = "SELECT m." + DatabaseHelper.MAST_CODE + ", " +
                "m." + DatabaseHelper.EMP_ID + ", " +
                "m." + DatabaseHelper.EMP_NAME + ", " +
                "m." + DatabaseHelper.DESIGNATION + ", " +
                "m." + DatabaseHelper.DEPARTMENT + ", " +
                "m." + DatabaseHelper.JOINED_DATE + ", " +
                "m." + DatabaseHelper.SALARY + ", " +
                "d." + DatabaseHelper.ADDRESS_LINE1 + ", " +
                "d." + DatabaseHelper.ADDRESS_LINE2 + ", " +
                "d." + DatabaseHelper.CITY + ", " +
                "d." + DatabaseHelper.STATE + ", " +
                "d." + DatabaseHelper.COUNTRY + " " +
                "FROM " + DatabaseHelper.TABLE_MASTER + " m " +
                "LEFT JOIN " + DatabaseHelper.TABLE_DETAIL + " d " +
                "ON m." + DatabaseHelper.MAST_CODE + " = d." + DatabaseHelper.EMP_CODE;

        Cursor cursor = database.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Employee employee = new Employee();
                employee.setMastCode(cursor.getInt(0));
                employee.setEmpId(cursor.getString(1));
                employee.setEmpName(cursor.getString(2));
                employee.setDesignation(cursor.getString(3));
                employee.setDepartment(cursor.getString(4));
                employee.setJoinedDate(cursor.getString(5));
                employee.setSalary(cursor.getDouble(6));
                employee.setAddressLine1(cursor.getString(7));
                employee.setAddressLine2(cursor.getString(8));
                employee.setCity(cursor.getString(9));
                employee.setState(cursor.getString(10));
                employee.setCountry(cursor.getString(11));

                employees.add(employee);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return employees;
    }

    // Update Employee
    public int updateEmployee(Employee employee) {
        database.beginTransaction();
        try {
            // Update Master
            ContentValues masterValues = new ContentValues();
            masterValues.put(DatabaseHelper.EMP_ID, employee.getEmpId());
            masterValues.put(DatabaseHelper.EMP_NAME, employee.getEmpName());
            masterValues.put(DatabaseHelper.DESIGNATION, employee.getDesignation());
            masterValues.put(DatabaseHelper.DEPARTMENT, employee.getDepartment());
            masterValues.put(DatabaseHelper.JOINED_DATE, employee.getJoinedDate());
            masterValues.put(DatabaseHelper.SALARY, employee.getSalary());

            int masterResult = database.update(DatabaseHelper.TABLE_MASTER, masterValues,
                    DatabaseHelper.MAST_CODE + " = ?",
                    new String[]{String.valueOf(employee.getMastCode())});

            // Update Detail
            ContentValues detailValues = new ContentValues();
            detailValues.put(DatabaseHelper.ADDRESS_LINE1, employee.getAddressLine1());
            detailValues.put(DatabaseHelper.ADDRESS_LINE2, employee.getAddressLine2());
            detailValues.put(DatabaseHelper.CITY, employee.getCity());
            detailValues.put(DatabaseHelper.STATE, employee.getState());
            detailValues.put(DatabaseHelper.COUNTRY, employee.getCountry());

            int detailResult = database.update(DatabaseHelper.TABLE_DETAIL, detailValues,
                    DatabaseHelper.EMP_CODE + " = ?",
                    new String[]{String.valueOf(employee.getMastCode())});

            if (masterResult > 0) {
                database.setTransactionSuccessful();
                return masterResult;
            }
            return 0;
        } finally {
            database.endTransaction();
        }
    }

    // Delete Employee (Master - Detail will cascade)
    public void deleteEmployee(int mastCode) {
        database.delete(DatabaseHelper.TABLE_MASTER,
                DatabaseHelper.MAST_CODE + " = ?",
                new String[]{String.valueOf(mastCode)});
    }
}
