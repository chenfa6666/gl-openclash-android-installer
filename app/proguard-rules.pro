# Add project specific ProGuard rules here.
# JSch: keep JSch core classes (used via reflection for cipher factories)
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
