# XPro release rules (R8). Keep tuned to libraries bundled in the app.
#
# Rule of thumb: keep what is reached by reflection, not whole app packages. The previous version
# kept `com.example.feature.**`, `com.example.viewmodel.**`, `com.example.data.model.**` and
# `com.devcode.xpro.mediadownloader.**` wholesale, which disabled shrinking and obfuscation for
# most of the app while still paying the R8 cost. `:app:assembleRelease` now runs in CI, so a
# missing keep rule shows up there instead of in a production crash.

# Keep line numbers for crash stack traces; hide original file names.
-keepattributes SourceFile,LineNumberTable,AnnotationDefault,EnclosingMethod,Signature,InnerClasses
-renamesourcefileattribute SourceFile

# --- Firebase / Firestore ---
# DTOs are mapped by the Firestore SDK via reflection (field names must survive).
-keep class com.example.data.remote.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- androidx.annotation.Keep (explicit opt-in survives shrinking) ---
-keep @androidx.annotation.Keep class * { *; }

# --- ViewModels ---
# viewModel()/ViewModelProvider instantiate these reflectively; class names may be obfuscated but
# the constructors and their signatures must be retained.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(android.app.Application);
    <init>();
}

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
