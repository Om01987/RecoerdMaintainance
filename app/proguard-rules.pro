# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# 1. Preserve all app classes (prevent stripping)
-keep class com.example.recordmaintenance.** { *; }

# 2. Preserve Activities & Fragments (used via Android manifest/reflection)
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment

# 3. Preserve your custom Print Adapters (reflection or instantiation)
-keep public class com.example.recordmaintenance.ListPrintAdapter { *; }
-keep public class com.example.recordmaintenance.ProfilePrintAdapter { *; }

# 4. Preserve EmployeeAdapter (RecyclerView adapter callbacks used reflectively)
-keep public class com.example.recordmaintenance.EmployeeAdapter { *; }

# 5. Preserve data model fields & methods (e.g., CSV writer, JSON)
-keepclassmembers class com.example.recordmaintenance.Employee {
    <fields>;
    <methods>;
}

# 6. Preserve Picasso classes (library uses reflection & annotations)
-keep class com.squareup.picasso.** { *; }
-keep interface com.squareup.picasso.** { *; }

# 7. Suppress warnings for common AndroidX & support packages
-dontwarn android.support.**
-dontwarn androidx.appcompat.**
-dontwarn androidx.core.**
-dontwarn com.google.android.material.**
-dontwarn de.hdodenhof.circleimageview.**

# 8. Preserve @Keep annotations usage
-keep @interface androidx.annotation.Keep
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# 9. Preserve FileProvider (used via manifest meta-data)
-keep public class androidx.core.content.FileProvider

# 10. Strip debug logging from production
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# 11. Bytecode optimization tuning
-optimizations !code/simplification/arithmetic
