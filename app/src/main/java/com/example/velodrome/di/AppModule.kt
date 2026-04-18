package com.example.velodrome.di

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "velodrome_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val DEFAULT_URL = "https://your-navidrome-server.com/"
    private const val TIMEOUT = 30L

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // First add the auth interceptor (adds u, t, s, v, c to all requests)
        val authInterceptor = AuthInterceptor

        // Logging interceptor for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // URL rewriting interceptor - uses stored server URL from CredentialsManager
        val urlRewriterInterceptor = okhttp3.Interceptor { chain ->
            val originalRequest = chain.request()
            val serverUrl = CredentialsManager.getServerUrl() ?: DEFAULT_URL
            
            val originalUrl = originalRequest.url.toString()
            
            // Rewrite if using placeholder or relative URL
            val newUrl = if (originalUrl.contains("your-navidrome-server.com")) {
                originalUrl.replace("https://your-navidrome-server.com/", serverUrl)
                    .replace("http://your-navidrome-server.com/", serverUrl)
            } else {
                originalUrl
            }

            if (newUrl != originalUrl) {
                Log.d("AppModule", "Rewriting URL: $originalUrl -> $newUrl")
            }

            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build()

            chain.proceed(newRequest)
        }

        return OkHttpClient.Builder()
            .addInterceptor(urlRewriterInterceptor)
            .addInterceptor(authInterceptor)  // Adds auth params to ALL requests
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        // Use default URL - actual URL comes from CredentialsManager via interceptor
        return Retrofit.Builder()
            .baseUrl(DEFAULT_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideNavidromeApi(retrofit: Retrofit): NavidromeApi {
        return retrofit.create(NavidromeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
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