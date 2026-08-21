package com.fergolde.velodrome.di

import android.content.Context
import android.content.SharedPreferences
import com.fergolde.velodrome.BuildConfig
import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.util.AuthInterceptor
import com.fergolde.velodrome.util.CredentialsManager
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.spght.encryptedprefs.EncryptedSharedPreferences
import dev.spght.encryptedprefs.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val DEFAULT_URL = "https://your-navidrome-server.com/"
    private const val TIMEOUT = 30L

    // -------------------------
    // CACHE PREFS (Para audio/img limit)
    // -------------------------
    @Provides
    @Singleton
    @Named("cache_prefs")
    fun provideCacheSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("velodrome_cache_prefs", Context.MODE_PRIVATE)
    }

    // -------------------------
    // APP SCOPE (Coroutines)
    // -------------------------
    @Provides
    @Singleton
    @Named("app_scope")
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // -------------------------
    // MOSHI
    // -------------------------
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    // -------------------------
    // OKHTTP
    // -------------------------
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        credentialsManager: CredentialsManager
    ): OkHttpClient {
        val urlRewriterInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url.toString()
            val rawServerUrl = credentialsManager.getServerUrl()
            val serverUrl = if (rawServerUrl.isNullOrBlank()) DEFAULT_URL else rawServerUrl.trim().let { if (it.endsWith("/")) it else "$it/" }

            val newUrl = if (originalUrl.contains("your-navidrome-server.com")) {
                originalUrl.replace("https://your-navidrome-server.com/rest/", serverUrl + "rest/")
                    .replace("http://your-navidrome-server.com/rest/", serverUrl + "rest/")
            } else {
                originalUrl
            }

            val safeHttpUrl = newUrl.toHttpUrlOrNull() ?: throw IOException("URL malformada: $newUrl")
            chain.proceed(originalRequest.newBuilder().url(safeHttpUrl).build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(urlRewriterInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE })
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    // -------------------------
    // RETROFIT & API
    // -------------------------
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder().baseUrl(DEFAULT_URL).client(okHttpClient).addConverterFactory(MoshiConverterFactory.create(moshi)).build()

    @Provides
    @Singleton
    fun provideNavidromeApi(retrofit: Retrofit): NavidromeApi = retrofit.create(NavidromeApi::class.java)

    // -------------------------
    // ENCRYPTED PREFS (Para credenciales)
    // -------------------------
    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context,
            "velodrome_encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
