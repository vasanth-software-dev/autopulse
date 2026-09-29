# AutoPulse Proguard / R8 Optimization Rules

# 1. Preserve Room Database & DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract *;
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.TypeConverter { *; }
-dontwarn androidx.room.paging.**

# 2. Preserve Data & Event Models (Avoid field renaming during serialization)
-keep class com.autopulse.automation.data.model.** { *; }
-keep class com.autopulse.automation.event.** { *; }
-keep class com.autopulse.automation.telegram.** { *; }
-keep class com.autopulse.automation.util.AppInfo { *; }

# 3. Gson Reflection Rules
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers enum * { *; }
-keep class com.google.gson.** { *; }

# 4. OkHttp & Okio Network Client Rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# 5. Kotlin Coroutines Optimization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# 6. Security Crypto (EncryptedSharedPreferences)
-keepclassmembers class androidx.security.crypto.** { *; }
