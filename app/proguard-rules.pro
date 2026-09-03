# ===================== OpenClash 安装器 ProGuard 规则 =====================
# 策略：只保留反射加载的类，让 R8 自由 tree-shake 其余代码。
# 之前的 -keep @kotlin.Metadata class * { *; } 会保留所有 Kotlin 类 → tree-shake 失效。

# ---------- JSch (mwiede fork) ----------
# JSch 用 Class.forName 按字符串名加载 KeyExchange/Cipher/MAC/Compression 算法实现，
# R8 无法静态追踪字符串引用 → 必须保留全部算法类，否则 SSH 握手 NoSuchMethodException。
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# ---------- OkHttp / Okio ----------
# OkHttp Platform 类用 Class.forName 探测 JDK/Conscrypt 平台 TLS，只保留探测链。
# 其余 OkHttp 类（logging, mockwebserver 等）让 R8 自由 shrink。
-keep class okhttp3.internal.platform.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------- Kotlin Coroutines ----------
# Kotlin Gradle 插件已自带协程状态机 keep 规则，此处仅防 warn + 保留状态字段。
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.*State* { *; }

# ---------- Kotlin Metadata ----------
# 只保留注解类本身；不要 keep 所有带 @Metadata 的类（那会禁用全部 Kotlin tree-shake）。
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlin.Metadata *;
}

# ---------- DataStore Preferences ----------
# Preferences DataStore 不使用 protobuf（自有序列化格式），protobuf 可被 R8 移除。
-keep class androidx.datastore.preferences.** { *; }
-dontwarn androidx.datastore.**
-dontwarn com.google.protobuf.**

# ---------- App 入口 / 反射调用的类 ----------
-keep class com.chenfa.openclashinstaller.App { *; }
-keep class com.chenfa.openclashinstaller.MainActivity { *; }
-keep class com.chenfa.openclashinstaller.** { <init>(); }
-keep class com.chenfa.openclashinstaller.data.model.** { *; }
-keep class com.chenfa.openclashinstaller.core.Constants { *; }

# ---------- 通用 ----------
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,Exceptions
-keepclasseswithmembernames class * { native <methods>; }
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
