package com.example.recordmaintenance;

public class Employee {
    // Firebase UID (primary identifier)
    private String uid;

    // Legacy mastCode for compatibility (can be removed later)
    private int mastCode;

    // Employee fields
    private String empId;
    private String empName;
    private String empEmail;
    private String role; // "admin" or "employee"
    private String designation;
    private String department;
    private String joinedDate;
    private double salary;
    private boolean passwordChanged;
    private String profilePhotoPath;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;

    // Firebase-specific fields
    private String createdAt;
    private String updatedAt;
    private String createdBy;

    // Constructors
    public Employee() {}

    public Employee(String empId, String empName, String empEmail, String role,
                    String designation, String department, String joinedDate, double salary,
                    String addressLine1, String addressLine2, String city, String state, String country) {
        this.empId = empId;
        this.empName = empName;
        this.empEmail = empEmail;
        this.role = role;
        this.designation = designation;
        this.department = department;
        this.joinedDate = joinedDate;
        this.salary = salary;
        this.passwordChanged = false;
        this.profilePhotoPath = null;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.country = country;
    }

    // Firebase UID getters and setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    // Legacy mastCode for compatibility
    public int getMastCode() { return mastCode; }
    public void setMastCode(int mastCode) { this.mastCode = mastCode; }

    // Employee ID
    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }

    // Name
    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }

    // Email
    public String getEmpEmail() { return empEmail; }
    public void setEmpEmail(String empEmail) { this.empEmail = empEmail; }

    // Role (admin/employee)
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // Password-related methods (for legacy compatibility)
    public String getEmpPassword() { return null; } // No longer used
    public void setEmpPassword(String empPassword) { /* No-op */ }

    public boolean isPasswordChanged() { return passwordChanged; }
    public void setPasswordChanged(boolean passwordChanged) { this.passwordChanged = passwordChanged; }

    // Profile photo
    public String getProfilePhotoPath() { return profilePhotoPath; }
    public void setProfilePhotoPath(String profilePhotoPath) { this.profilePhotoPath = profilePhotoPath; }

    // Job information
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getJoinedDate() { return joinedDate; }
    public void setJoinedDate(String joinedDate) { this.joinedDate = joinedDate; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    // Address information
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

    // Firebase metadata
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}