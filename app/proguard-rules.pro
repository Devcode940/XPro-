# XPro release rules (R8). Keep tuned to libraries bundled in the app.

# Keep line numbers for crash stack traces; hide original file names.
-keepattributes SourceFile,LineNumberTable,AnnotationDefault,EnclosingMethod,Signature,InnerClasses
-renamesourcefileattribute SourceFile

# --- Firebase / Firestore ---
# DTOs are mapped by the Firestore SDK via reflection.
-keep class com.example.data.remote.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Parcelable / Room ---
-keep class * implements android.os.Parcelable { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# --- kotlinx.coroutines / Kotlin metadata ---
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# --- Credential Manager / Google Sign-In ---
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-dontwarn androidx.credentials.**

# --- Compose ---
-dontwarn androidx.compose.**

# --- App models (domain DTO-ish classes used via reflection/serialization) ---
-keep class com.example.data.model.** { *; }
-keep class com.example.feature.** { *; }
-keep class com.example.viewmodel.** { *; }
-keep class com.devcode.xpro.mediadownloader.** { *; }
