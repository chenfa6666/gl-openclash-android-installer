# ===================== OpenClash 安装器 ProGuard 规则 =====================

# ---------- JSch (mwiede fork) ----------
# JSch 通过反射加载 KeyExchange/Cipher/MAC/Compression 等算法实现类，
# R8 不知道这些类被反射使用，会删 → 连接 SSH 时 NoSuchMethodException。
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# ---------- OkHttp / Okio ----------
# OkHttp 用 Platform 反射查找 JDK 1.8+ HttpClient 等平台类，
# 同时 okio 用 ByteString 反射，必须保留。
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# ---------- Kotlin Coroutines ----------
# 协程挂起恢复时通过 Continuation 续体反射恢复 lambda 状态，
# 去掉会导致 launchOp / ensureActiveOrCancel 在边界处失败。
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class kotlin.coroutines.** { *; }
-keep class kotlin.coroutines.intrinsics.** { *; }
# 协程状态机：带 $| 状态字段的 lambda 闭包类不能被删
-keepclassmembers class kotlin.coroutines.SafeContinuation { *; }
-keepclassmembers class kotlinx.coroutines.*State* { *; }

# ---------- Kotlin Metadata ----------
# 反射（DataStore / 协程）需要 @Metadata 注解内容读取 Kotlin 类型信息
-keep @kotlin.Metadata class * { *; }
-keepclassmembers class * {
    @kotlin.Metadata *;
}

# ---------- DataStore Preferences ----------
# Preferences DataStore 内部用 protobuf-like 序列化反序列化键值
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# ---------- Compose ----------
# Compose Compiler 已自带规则；保险起见保留 runtime / material3
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# ---------- App 入口 / 反射调用的类 ----------
# Application / MainActivity 通过 AndroidManifest 反射实例化
-keep class com.chenfa.openclashinstaller.App { *; }
-keep class com.chenfa.openclashinstaller.MainActivity { *; }
-keep class com.chenfa.openclashinstaller.** { <init>(); }
-keep class com.chenfa.openclashinstaller.data.model.** { *; }
-keep class com.chenfa.openclashinstaller.core.Constants { *; }

# ---------- 通用：保留注解、原生方法、枚举 ----------
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes Exceptions

-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
