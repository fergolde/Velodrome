package com.example.velodrome.di

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.util.AuthInterceptor
import com.example.velodrome.util.CredentialsManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.spght.encryptedprefs.EncryptedSharedPreferences
import dev.spght.encryptedprefs.MasterKey
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "velodrome_prefs"
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val DEFAULT_URL = "https://your-navidrome-server.com/"
    private const val TIMEOUT = 30L

    // -------------------------
    // MOSHI
    // -------------------------

    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

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

            // 🔥 SAFE SERVER URL (NO CRASHES)
            val rawServerUrl = credentialsManager.getServerUrl()

            val serverUrl = if (rawServerUrl.isNullOrBlank()) {
                DEFAULT_URL
            } else {
                rawServerUrl.trim().let {
                    if (it.endsWith("/")) it else "$it/"
                }
            }

            val newUrl = if (originalUrl.contains("your-navidrome-server.com")) {
                originalUrl
                    .replace(
                        "https://your-navidrome-server.com/rest/",
                        serverUrl + "rest/"
                    )
                    .replace(
                        "http://your-navidrome-server.com/rest/",
                        serverUrl + "rest/"
                    )
            } else {
                originalUrl
            }

            if (newUrl != originalUrl) {
                Log.d("AppModule", "URL rewrite: $originalUrl -> $newUrl")
            }

            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build()

            chain.proceed(newRequest)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(urlRewriterInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    // -------------------------
    // RETROFIT
    // -------------------------

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DEFAULT_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // -------------------------
    // API
    // -------------------------

    @Provides
    @Singleton
    fun provideNavidromeApi(retrofit: Retrofit): NavidromeApi =
        retrofit.create(NavidromeApi::class.java)

    // -------------------------
    // DATASTORE
    // -------------------------

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    // -------------------------
    // ENCRYPTED PREFS
    // -------------------------

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "velodrome_encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}