# Room, Compose, kotlinx.serialization and Coil ship consumer rules; add app-specific keeps here only when needed.
# OkHttp's optional TLS providers are not on Android's classpath.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.internal.platform.**
# Keep line numbers so crash reports (Settings > Last crash) stay readable.
-keepattributes SourceFile,LineNumberTable
