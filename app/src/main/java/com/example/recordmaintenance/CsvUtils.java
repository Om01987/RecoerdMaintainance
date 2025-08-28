package com.example.recordmaintenance;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utility class for writing CSV files with proper RFC 4180 escaping
 * Handles commas, quotes, and newlines in field values
 */
public final class CsvUtils {

    private CsvUtils() {}

    /**
     * Writes a list of employees to a CSV OutputStream with UTF-8 encoding
     * @param out The OutputStream to write to
     * @param employees List of employees to export
     * @throws Exception if writing fails
     */
    public static void writeEmployeesToCsv(OutputStream out, List<Employee> employees) throws Exception {
        // Optional: Add UTF-8 BOM for Excel compatibility (uncomment if needed)
        // out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // Write CSV header
            writer.write("EmpID,Name,Email,Department,Designation,JoinedDate,Salary,City,State,Country,PasswordChanged");
            writer.newLine();

            // Write employee data
            if (employees != null) {
                for (Employee employee : employees) {
                    String[] fields = new String[] {
                            nullToEmpty(employee.getEmpId()),
                            nullToEmpty(employee.getEmpName()),
                            nullToEmpty(employee.getEmpEmail()),
                            nullToEmpty(employee.getDepartment()),
                            nullToEmpty(employee.getDesignation()),
                            nullToEmpty(employee.getJoinedDate()),
                            String.valueOf(employee.getSalary()),
                            nullToEmpty(employee.getCity()),
                            nullToEmpty(employee.getState()),
                            nullToEmpty(employee.getCountry()),
                            employee.isPasswordChanged() ? "Yes" : "No"
                    };

                    writer.write(joinCsvFields(fields));
                    writer.newLine();
                }
            }
            writer.flush();
        }
    }

    /**
     * Converts null strings to empty strings
     */
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Joins CSV fields with proper escaping
     */
    private static String joinCsvFields(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsvField(fields[i]));
        }
        return sb.toString();
    }

    /**
     * Escapes a CSV field according to RFC 4180:
     * - Fields containing commas, quotes, or newlines are enclosed in double quotes
     * - Double quotes within fields are escaped by doubling them
     */
    private static String escapeCsvField(String field) {
        boolean needsQuoting = field.contains(",") ||
                field.contains("\"") ||
                field.contains("\n") ||
                field.contains("\r");

        if (!needsQuoting) {
            return field;
        }

        // Escape existing quotes by doubling them
        String escaped = field.replace("\"", "\"\"");

        // Wrap in quotes
        return "\"" + escaped + "\"";
    }
}
