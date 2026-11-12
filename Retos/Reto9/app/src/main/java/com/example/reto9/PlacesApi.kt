package com.example.reto9

import android.location.Location
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos

/**
 * Finds named points of interest around a position with Nominatim,
 * OpenStreetMap's search service. [tag] is an OSM tag such as
 * "amenity=hospital". Blocking: call it from a background thread.
 */
object PlacesApi {

    /** Identifies the app to the OpenStreetMap servers (they block generic ones). */
    const val USER_AGENT = "Reto9-DADM-UNAL/1.0"

    private const val URL_SEARCH = "https://nominatim.openstreetmap.org/search"

    fun search(tag: String, lat: Double, lon: Double, radiusMeters: Int): List<Place> {
        val (key, value) = tag.split("=", limit = 2)
        // Nominatim searches inside a box; we keep only what falls in the circle
        val dLat = radiusMeters / 111_320.0
        val dLon = radiusMeters / (111_320.0 * cos(Math.toRadians(lat)))
        val url = "$URL_SEARCH?format=jsonv2&limit=40&bounded=1&q=$value" +
            "&viewbox=${lon - dLon},${lat + dLat},${lon + dLon},${lat - dLat}"

        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept-Language", "es")
            if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            return parse(json, key, value).filter { distanceMeters(lat, lon, it) <= radiusMeters }
        } finally {
            conn.disconnect()
        }
    }

    /** Keeps the named results whose OSM tag is exactly key=value. */
    private fun parse(json: String, key: String, value: String): List<Place> {
        val results = JSONArray(json)
        val places = ArrayList<Place>()
        for (i in 0 until results.length()) {
            val r = results.getJSONObject(i)
            val name = r.optString("name")
            if (name.isBlank() || r.optString("category") != key || r.optString("type") != value) continue
            places.add(Place(name, r.getString("lat").toDouble(), r.getString("lon").toDouble()))
        }
        return places
    }

    private fun distanceMeters(lat: Double, lon: Double, place: Place): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat, lon, place.lat, place.lon, result)
        return result[0]
    }
}
