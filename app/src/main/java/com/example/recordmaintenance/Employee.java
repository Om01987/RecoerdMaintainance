package com.example.recordmaintenance;

public class Employee {
    private int mastCode;
    private String empId;
    private String empName;
    private String designation;
    private String department;
    private String joinedDate;
    private double salary;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;

    // Constructor
    public Employee() {}

    public Employee(String empId, String empName, String designation,
                    String department, String joinedDate, double salary,
                    String addressLine1, String addressLine2, String city,
                    String state, String country) {
        this.empId = empId;
        this.empName = empName;
        this.designation = designation;
        this.department = department;
        this.joinedDate = joinedDate;
        this.salary = salary;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.country = country;
    }

    // Getters and Setters
    public int getMastCode() { return mastCode; }
    public void setMastCode(int mastCode) { this.mastCode = mastCode; }

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }

    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getJoinedDate() { return joinedDate; }
    public void setJoinedDate(String joinedDate) { this.joinedDate = joinedDate; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
