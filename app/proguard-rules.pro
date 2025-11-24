# Add project specific ProGuard rules here.

# Keep data classes for Gson
-keepclassmembers class com.bybit.rebalancer.data.model.** { *; }
-keepclassmembers class com.bybit.rebalancer.api.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
