package com.example.recordmaintenance;

public class Employee {
    private int mastCode;
    private String empId;
    private String empName;
    private String empEmail; // New field
    private String empPassword; // New field (hashed)
    private String designation;
    private String department;
    private String joinedDate;
    private double salary;
    private boolean passwordChanged; // New field
    private String profilePhotoPath; // NEW: Profile photo path
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;

    // Constructors
    public Employee() {}

    public Employee(String empId, String empName, String empEmail, String empPassword,
                    String designation, String department, String joinedDate, double salary,
                    String addressLine1, String addressLine2, String city, String state, String country) {
        this.empId = empId;
        this.empName = empName;
        this.empEmail = empEmail;
        this.empPassword = empPassword;
        this.designation = designation;
        this.department = department;
        this.joinedDate = joinedDate;
        this.salary = salary;
        this.passwordChanged = false; // Default to false for new employees
        this.profilePhotoPath = null; // Default to null for new employees
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.country = country;
    }

    // All existing getters and setters...
    public int getMastCode() { return mastCode; }
    public void setMastCode(int mastCode) { this.mastCode = mastCode; }

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }

    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }

    // New getters and setters
    public String getEmpEmail() { return empEmail; }
    public void setEmpEmail(String empEmail) { this.empEmail = empEmail; }

    public String getEmpPassword() { return empPassword; }
    public void setEmpPassword(String empPassword) { this.empPassword = empPassword; }

    public boolean isPasswordChanged() { return passwordChanged; }
    public void setPasswordChanged(boolean passwordChanged) { this.passwordChanged = passwordChanged; }

    // NEW: Profile photo getters and setters
    public String getProfilePhotoPath() { return profilePhotoPath; }
    public void setProfilePhotoPath(String profilePhotoPath) { this.profilePhotoPath = profilePhotoPath; }

    // Rest of existing getters and setters remain the same...
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