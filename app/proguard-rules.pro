# ---- yt-dlp / ffmpeg / aria2c engine (reached through JNI) ----
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**
-dontwarn com.yausername.youtubedl_android.**

# ---- WebView JavaScript bridge (used by the video detector) ----
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# ---- App model / storage classes ----
-keep class com.lonetube.app.data.** { *; }
-keep class com.lonetube.app.engine.** { *; }

# ---- Kotlin metadata kept for reflection safety ----
-keepattributes Signature,InnerClasses,EnclosingMethod
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
