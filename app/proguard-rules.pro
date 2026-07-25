# ATU Cafeteria Mobile Application - Release ProGuard / R8 Obfuscation & Shrinking Rules

# 1. Preserve Line Numbers & Source Files for Crashlytics Stack Traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. Preserve Custom Annotations & Reflection Attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations

# 3. Retrofit & OkHttp Networking Rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# 4. Moshi & Gson JSON Serialization / Reflection Models
-keepattributes Signature
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
}

# 5. Preserve Data Models (DTOs, Room Entities, Network Data Classes)
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-keep class com.example.data.models.** { *; }

# 6. Room Database Engine & KSP Generated Code Rules
-keep class * extends androidx.room.RoomDatabase
-keep class *_Impl extends androidx.room.RoomDatabase
-keep class com.example.data.local.dao.** { *; }
-dontwarn androidx.room.paging.**

# 7. WorkManager & Background Tasks
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# 8. Hilt & Dependency Injection
-keep class * extends android.app.Application
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.lifecycle.ViewModel

# 9. Kotlin Coroutines & Flow
-keepclassmembers class kotlinx.coroutines.android.HandlerDispatcher {
    <init>(...);
}
-dontwarn kotlinx.coroutines.**

# 10. Android Jetpack Compose & Material 3
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
