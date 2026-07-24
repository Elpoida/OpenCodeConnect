# Keep Retrofit and OkHttp
-keepattributes Signature, *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * implements kotlinx.serialization.KSerializer { *; }

# Keep data models
-keep class com.opencode.thin.data.model.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
