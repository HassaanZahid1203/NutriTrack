# Default ProGuard rules for NutriTrack.
# Add app-specific rules here. Most defaults from proguard-android-optimize.txt are sufficient.

# Keep Firestore data classes (constructors and field-access via reflection).
-keep class com.nutritrack.app.models.** { *; }
-keepclassmembers class com.nutritrack.app.models.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
