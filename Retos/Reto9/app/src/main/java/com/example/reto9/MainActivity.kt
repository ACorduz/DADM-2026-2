package com.example.reto9

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.concurrent.Executors
import kotlin.math.floor
import kotlin.math.log2

/**
 * Gets the user's GPS position and shows on an OpenStreetMap map the
 * points of interest (from the Overpass API) within the radius set in
 * the preferences. Each marker shows the place's name when tapped.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mMap: MapView
    private lateinit var mStatus: TextView
    private lateinit var mLocationManager: LocationManager
    private val mExecutor = Executors.newSingleThreadExecutor()

    private var mLocation: Location? = null
    /** Radius and category of the last search, to redo it when they change. */
    private var mLastSearch: Pair<Double, String>? = null

    private val mLocationListener = LocationListener { location ->
        stopLocationUpdates()
        onLocation(location)
    }

    private val mPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) requestLocation() else mStatus.setText(R.string.no_permission)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tile servers reject generic user agents such as "com.example..."
        Configuration.getInstance().userAgentValue = PlacesApi.USER_AGENT
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        mStatus = findViewById(R.id.status)
        mMap = findViewById(R.id.map)
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.overlays.add(CopyrightOverlay(this))
        mMap.setMultiTouchControls(true)
        mMap.controller.setZoom(15.0)
        mMap.controller.setCenter(GeoPoint(4.6371, -74.0828)) // Bogotá until we know where we are

        mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        requestLocation()
    }

    override fun onResume() {
        super.onResume()
        mMap.onResume()
        // Coming back from the preferences: search again if something changed
        val loc = mLocation
        if (loc != null && mLastSearch != Prefs.radiusKm(this) to Prefs.category(this)) {
            showPlaces(loc)
        }
    }

    override fun onPause() {
        super.onPause()
        mMap.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        mExecutor.shutdownNow()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> { requestLocation(); true }
        R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    /** Asks for permission if needed, then waits for one location fix. */
    @SuppressLint("MissingPermission")
    private fun requestLocation() {
        val fine = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!fine && !coarse) {
            mPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { mLocationManager.isProviderEnabled(it) && (fine || it != LocationManager.GPS_PROVIDER) }
        if (providers.isEmpty()) {
            mStatus.setText(R.string.location_off)
            return
        }
        mStatus.setText(R.string.waiting_location)
        // Show the last known position right away, then refine with a fresh fix
        providers.mapNotNull { mLocationManager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?.let { onLocation(it) }
        stopLocationUpdates()
        for (provider in providers) {
            mLocationManager.requestLocationUpdates(provider, 0L, 0f, mLocationListener, Looper.getMainLooper())
        }
    }

    private fun stopLocationUpdates() {
        mLocationManager.removeUpdates(mLocationListener)
    }

    private fun onLocation(location: Location) {
        mLocation = location
        showPlaces(location)
    }

    /** Draws the user's position and the radius, then loads the places in the background. */
    private fun showPlaces(location: Location) {
        val radiusKm = Prefs.radiusKm(this)
        val category = Prefs.category(this)
        val search = radiusKm to category
        mLastSearch = search
        val me = GeoPoint(location.latitude, location.longitude)

        mMap.overlays.clear()
        mMap.overlays.add(CopyrightOverlay(this))
        mMap.overlays.add(Polygon(mMap).apply {
            points = Polygon.pointsAsCircle(me, radiusKm * 1000)
            fillPaint.color = Color.argb(40, 33, 150, 243)
            outlinePaint.color = Color.rgb(33, 150, 243)
            outlinePaint.strokeWidth = 3f
            infoWindow = null
        })
        mMap.overlays.add(Marker(mMap).apply {
            position = me
            title = getString(R.string.you_are_here)
            icon = ContextCompat.getDrawable(this@MainActivity, org.osmdroid.library.R.drawable.person)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        })
        mMap.controller.setZoom(zoomFor(radiusKm))
        mMap.controller.setCenter(me)
        mMap.invalidate()
        mStatus.setText(R.string.loading_places)

        mExecutor.execute {
            val places = try {
                PlacesApi.search(category, me.latitude, me.longitude, (radiusKm * 1000).toInt())
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                // Ignore the results of an older search
                if (isDestroyed || mLastSearch != search || mLocation !== location) return@runOnUiThread
                if (places == null) {
                    mStatus.setText(R.string.load_failed)
                } else {
                    addPlaceMarkers(places, location)
                    mStatus.text = getString(R.string.status, me.latitude, me.longitude,
                        places.size, formatKm(radiusKm))
                }
            }
        }
    }

    private fun addPlaceMarkers(places: List<Place>, me: Location) {
        val distance = FloatArray(1)
        for (place in places) {
            Location.distanceBetween(me.latitude, me.longitude, place.lat, place.lon, distance)
            mMap.overlays.add(Marker(mMap).apply {
                position = GeoPoint(place.lat, place.lon)
                title = place.name
                snippet = getString(R.string.distance, distance[0] / 1000)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
        }
        mMap.invalidate()
    }

    /** A zoom level that fits the whole search circle on screen. */
    private fun zoomFor(radiusKm: Double): Double = (15.0 - log2(radiusKm)).coerceIn(3.0, 18.0)

    private fun formatKm(km: Double) =
        if (km == floor(km)) km.toInt().toString() else km.toString()
}
