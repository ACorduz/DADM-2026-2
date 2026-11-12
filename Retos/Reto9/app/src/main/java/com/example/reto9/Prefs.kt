package com.example.reto9

import android.content.Context
import androidx.preference.PreferenceManager

/** Reads the values chosen in [SettingsActivity]. */
object Prefs {
    const val KEY_RADIUS = "radius"
    const val KEY_CATEGORY = "category"
    private const val DEFAULT_RADIUS = "1"
    private const val DEFAULT_CATEGORY = "amenity=hospital"

    fun isValidRadius(text: String): Boolean =
        text.trim().replace(',', '.').toDoubleOrNull()?.let { it in 0.1..50.0 } ?: false

    fun radiusKm(context: Context): Double {
        val text = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_RADIUS, DEFAULT_RADIUS) ?: DEFAULT_RADIUS
        return if (isValidRadius(text)) text.trim().replace(',', '.').toDouble() else 1.0
    }

    fun category(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_CATEGORY, DEFAULT_CATEGORY) ?: DEFAULT_CATEGORY
}
