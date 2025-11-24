# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Retrofit
-keepattributes Signature
-keepattributes Exceptions

# Keep Gson
-keep class com.mahfuz.rebalancer.data.model.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
