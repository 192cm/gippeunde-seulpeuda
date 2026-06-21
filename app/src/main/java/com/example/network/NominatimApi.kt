package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OpenStreetMap Nominatim reverse-geocoding API (no key, no signup).
 * Docs: https://nominatim.org/release-docs/latest/api/Reverse/
 */
interface NominatimApi {
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2",
        @Query("accept-language") language: String = "ko",
        @Query("zoom") zoom: Int = 18,
    ): NominatimResponse
}

@JsonClass(generateAdapter = true)
data class NominatimResponse(
    @Json(name = "display_name") val displayName: String? = null,
    val name: String? = null,
    val address: NominatimAddress? = null,
)

@JsonClass(generateAdapter = true)
data class NominatimAddress(
    val amenity: String? = null,
    val building: String? = null,
    val road: String? = null,
    val neighbourhood: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val town: String? = null,
)

/**
 * Turns a coordinate into a human-readable Korean address.
 *
 * Every failure path (no network, timeout, malformed body) returns null so the
 * caller can fall back to the locally chosen hotspot name — the UI never breaks.
 */
object GeocodingRepository {

    suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val response = ApiProvider.nominatimApi.reverseGeocode(lat = lat, lon = lon)
            val addr = response.address
            // Prefer the most specific place name, fall back to the full display name.
            val concise = listOfNotNull(
                addr?.amenity ?: addr?.building,
                addr?.road ?: addr?.neighbourhood ?: addr?.suburb,
            ).joinToString(" ")
            concise.ifBlank { response.displayName }?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
