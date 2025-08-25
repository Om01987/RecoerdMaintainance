package com.example.recordmaintenance;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Information
    private static final String DB_NAME = "EmployeeRecords.db";
    private static final int DB_VERSION = 3; // Incremented for TblUserMaster addition

    // Table Names
    public static final String TABLE_USER_MASTER = "TblUserMaster";
    public static final String TABLE_MASTER = "TblEmployeeMaster";
    public static final String TABLE_DETAIL = "TblEmployeeDetail";

    // User Master Table Columns
    public static final String USER_MAST_CODE = "MastCode";
    public static final String USER_ID = "UserID";
    public static final String USER_PASSWORD = "Password";
    public static final String USER_NAME = "UserName";

    // Master Table Columns
    public static final String MAST_CODE = "MastCode";
    public static final String EMP_ID = "EmpID"; // Now follows MAN25#### pattern
    public static final String EMP_NAME = "EmpName";
    public static final String EMP_EMAIL = "EmpEmail"; // New field
    public static final String EMP_PASSWORD = "EmpPassword"; // New field (hashed)
    public static final String DESIGNATION = "Designation";
    public static final String DEPARTMENT = "Department";
    public static final String JOINED_DATE = "JoinedDate";
    public static final String SALARY = "Salary";
    public static final String PASSWORD_CHANGED = "PasswordChanged"; // Track if initial password changed

    // Detail Table Columns (unchanged)
    public static final String EMP_CODE = "EmpCode";
    public static final String ADDRESS_LINE1 = "AddressLine1";
    public static final String ADDRESS_LINE2 = "AddressLine2";
    public static final String CITY = "City";
    public static final String STATE = "State";
    public static final String COUNTRY = "Country";

    // Create User Master Table SQL
    private static final String CREATE_USER_MASTER_TABLE =
            "CREATE TABLE " + TABLE_USER_MASTER + "(" +
                    USER_MAST_CODE + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    USER_ID + " TEXT UNIQUE NOT NULL, " +
                    USER_PASSWORD + " TEXT NOT NULL, " +
                    USER_NAME + " TEXT NOT NULL" + ")";

    // Create Master Table SQL with new fields and unique constraint
    private static final String CREATE_MASTER_TABLE =
            "CREATE TABLE " + TABLE_MASTER + "(" +
                    MAST_CODE + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    EMP_ID + " TEXT UNIQUE NOT NULL, " +
                    EMP_NAME + " TEXT NOT NULL, " +
                    EMP_EMAIL + " TEXT UNIQUE NOT NULL, " +
                    EMP_PASSWORD + " TEXT NOT NULL, " +
                    DESIGNATION + " TEXT, " +
                    DEPARTMENT + " TEXT, " +
                    JOINED_DATE + " TEXT, " +
                    SALARY + " REAL, " +
                    PASSWORD_CHANGED + " INTEGER DEFAULT 0" + ")";

    // Create Detail Table SQL (unchanged)
    private static final String CREATE_DETAIL_TABLE =
            "CREATE TABLE " + TABLE_DETAIL + "(" +
                    EMP_CODE + " INTEGER UNIQUE, " +
                    ADDRESS_LINE1 + " TEXT, " +
                    ADDRESS_LINE2 + " TEXT, " +
                    CITY + " TEXT, " +
                    STATE + " TEXT, " +
                    COUNTRY + " TEXT, " +
                    "FOREIGN KEY(" + EMP_CODE + ") REFERENCES " +
                    TABLE_MASTER + "(" + MAST_CODE + ") ON DELETE CASCADE" + ")";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON;");
        db.execSQL(CREATE_USER_MASTER_TABLE);
        db.execSQL(CREATE_MASTER_TABLE);
        db.execSQL(CREATE_DETAIL_TABLE);

        // Insert default admin user
        insertDefaultAdmin(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Create TblUserMaster table
            db.execSQL(CREATE_USER_MASTER_TABLE);

            // Insert default admin user
            insertDefaultAdmin(db);
        }

        if (oldVersion < 2) {
            // Add new columns to existing table
            db.execSQL("ALTER TABLE " + TABLE_MASTER + " ADD COLUMN " + EMP_EMAIL + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_MASTER + " ADD COLUMN " + EMP_PASSWORD + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_MASTER + " ADD COLUMN " + PASSWORD_CHANGED + " INTEGER DEFAULT 0");

            // Make EMP_ID unique
            db.execSQL("CREATE UNIQUE INDEX idx_emp_id ON " + TABLE_MASTER + "(" + EMP_ID + ")");
            db.execSQL("CREATE UNIQUE INDEX idx_emp_email ON " + TABLE_MASTER + "(" + EMP_EMAIL + ")");
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    /**
     * Insert default admin user into TblUserMaster
     */
    private void insertDefaultAdmin(SQLiteDatabase db) {
        ContentValues adminValues = new ContentValues();
        adminValues.put(USER_ID, "admin@example.com");
        adminValues.put(USER_PASSWORD, hashPassword("Admin@123"));
        adminValues.put(USER_NAME, "System Administrator");

        try {
            db.insert(TABLE_USER_MASTER, null, adminValues);
        } catch (Exception e) {
            // Admin might already exist, ignore
        }
    }

    /**
     * Hash password using SHA-256
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password; // Fallback to plain text (not recommended for production)
        }
    }
}