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

# ── Hilt ──────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# ── Room ──────────────────────────────────────────────────────────────────────
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Google API Client / Sheets ────────────────────────────────────────────────
-keep class com.google.api.** { *; }
-keep class com.google.apis.** { *; }
-keep class com.google.http.** { *; }
-keep class com.google.auth.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.apis.**
-dontwarn com.google.common.**

# ── Kotlinx Serialization ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.example.todoit.**$$serializer { *; }
-keepclassmembers class com.example.todoit.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.todoit.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── WorkManager ───────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Suppress common warnings ──────────────────────────────────────────────────
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.android.internal.http.multipart.**

# ── Location & Geofencing ─────────────────────────────────────────────────────
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# ── BroadcastReceivers (must not be obfuscated) ───────────────────────────────
-keep class com.example.todoit.notification.BootReceiver { *; }
-keep class com.example.todoit.notification.ReminderBroadcastReceiver { *; }
-keep class com.example.todoit.notification.GeofenceBroadcastReceiver { *; }

