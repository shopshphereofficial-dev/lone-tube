# ---- Never obfuscate: names stay exactly as written ----
-dontobfuscate
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# ---- yt-dlp / ffmpeg / aria2c engine (reached through JNI) ----
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# ---- Our own code: keep everything (the app is small, safety first) ----
-keep class com.soulstream.** { *; }

# ---- WebView JavaScript bridges ----
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ---- Quiet the usual library noise ----
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
