package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Central Retrofit/OkHttp provider for every external API the app talks to.
 *
 * One shared OkHttp client (logging + sane timeouts + a polite User-Agent that
 * Nominatim's usage policy requires) is reused across separate Retrofit
 * instances, because each API lives on a different base URL.
 */
object ApiProvider {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // Nominatim rejects requests without an identifying User-Agent.
                val request = chain.request().newBuilder()
                    .header("User-Agent", "GippeundeSeulpeuda/1.0 (PNU SW2026 term project)")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    /** OpenStreetMap reverse-geocoding (no API key required). */
    val nominatimApi: NominatimApi by lazy {
        retrofit("https://nominatim.openstreetmap.org/").create(NominatimApi::class.java)
    }

    /** Google Gemini text generation. */
    val geminiApi: GeminiApi by lazy {
        retrofit("https://generativelanguage.googleapis.com/").create(GeminiApi::class.java)
    }
}
