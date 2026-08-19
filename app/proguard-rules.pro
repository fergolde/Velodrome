# ============================================================
# Velodrome ProGuard / R8 Rules
# ============================================================

# ---------- General ----------
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# ---------- Moshi ----------
# Keep Moshi-generated adapters (KSP codegen)
-keep class com.fergolde.velodrome.data.remote.dto.** { *; }
-keep class com.fergolde.velodrome.data.remote.dto.**$* { *; }

# Moshi reflection fallback (KotlinJsonAdapterFactory)
-keepclassmembers class com.fergolde.velodrome.data.remote.dto.** {
    <fields>;
    <methods>;
}
-keep class com.squareup.moshi.** { *; }
-keep class com.squareup.moshi.internal.Util { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <fields>;
}
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keep class * extends com.squareup.moshi.JsonAdapter$Factory

# ---------- Room ----------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.fergolde.velodrome.data.local.entity.** { *; }
-keep class com.fergolde.velodrome.data.local.dao.** { *; }
-keep class com.fergolde.velodrome.data.local.VelodromeDatabase { *; }
-keep class com.fergolde.velodrome.data.local.VelodromeDatabase$* { *; }

# Room type converters (if any)
-keep class com.fergolde.velodrome.data.local.**Converter { *; }

# ---------- Hilt / Dagger ----------
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewFragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.earlyentrypoint.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# Hilt generated components
-keep class **_HiltComponents* { *; }
-keep class **_GeneratedInjector { *; }
-keep class dagger.hilt.android.internal.** { *; }

# ---------- kotlinx.serialization ----------
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Transient <fields>;
}
-keep class kotlinx.serialization.json.** { *; }
-keep class kotlinx.serialization.KSerializer { *; }

# ---------- Retrofit / OkHttp ----------
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class * implements retrofit2.Converter.Factory
-keep,allowobfuscation,allowshrinking class * implements retrofit2.CallAdapter.Factory
-keep,allowobfuscation,allowshrinking class * implements okhttp3.Interceptor
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------- DataStore ----------
-keep class * extends androidx.datastore.core.Serializer { *; }

# ---------- Coil ----------
-keep class coil3.** { *; }
-dontwarn coil3.**

# ---------- ExoPlayer / Media3 ----------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ---------- WorkManager ----------
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class com.fergolde.velodrome.data.worker.** { *; }

# ---------- Enums ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Parcelable ----------
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ---------- Kotlin Serialization (navigation) ----------
-keepclassmembers class com.fergolde.velodrome.presentation.navigation.** {
    *** Companion;
}
-keepclassmembers class com.fergolde.velodrome.presentation.navigation.**$$serializer {
    *** INSTANCE;
}
