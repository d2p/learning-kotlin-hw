package com.ouluuni21.assistedreminder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.util.*

const val GEOFENCE_RADIUS = 200
const val GEOFENCE_ID = "REMINDER_GEOFENCE_ID"
const val GEOFENCE_EXPIRATION = 10 * 24 * 60 * 60 * 1000 // 10 days
const val GEOFENCE_DWELL_DELAY =  10 * 1000 // 10 secs
const val CAMERA_ZOOM_LEVEL = 13f

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val LOCATION_PERMISSION_CODE = 2001
    private val GEOFENCE_LOCATION_REQUEST_CODE = 2002
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient

    private lateinit var reminderMarker: Marker
    private lateinit var reminderArea: Circle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        val currentCity = LatLng(78.22210201886925, 15.639839392797546)
        map.addMarker(MarkerOptions().position(currentCity).title("Welcome to Svalbard"))

        enableMyLocation()

        if (map.isMyLocationEnabled) {
            // Zoom to last known location
            fusedLocationClient.lastLocation.addOnSuccessListener {
                val latLng = loadReminderLocation()
                if (latLng.latitude != 0.0) {
                    setReminderLocation(map, latLng)
                }
                else if (it != null && currentCity == null) {
                    with(map) {
                        val curLoc = LatLng(it.latitude, it.longitude)
                        moveCamera(CameraUpdateFactory.newLatLngZoom(curLoc, CAMERA_ZOOM_LEVEL))
                    }
                } else {
                    with(map) {
                        moveCamera(CameraUpdateFactory.newLatLngZoom(currentCity,CAMERA_ZOOM_LEVEL))
                    }
                }
            }
        }

        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.map_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun loadReminderLocation(): LatLng {
        // Very quick and dirty way to store and get latest marker info
        val sharedPref = applicationContext.getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE
        )
        val latitude = sharedPref.getFloat("latitude", 0.0F).toDouble()
        val longitude = sharedPref.getFloat("longitude", 0.0F).toDouble()
        return LatLng(latitude, longitude)
    }

    // Allow users to add markers on map with long click
    private fun setMapLongClick(map: GoogleMap) {
        // Very quick and dirty way to store latest marker info
        val sharedPref = applicationContext.getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE
        )

        map.setOnMapLongClickListener { latlng ->
            setReminderLocation(map, latlng)
            sharedPref.edit().putFloat("latitude", latlng.latitude.toFloat()).apply()
            sharedPref.edit().putFloat("longitude", latlng.longitude.toFloat()).apply()
/*
            val database = Firebase.database(getString(R.string.db_url))
            val reference = database.getReference("reminders")
            val key = reference.push().key
            if (key != null) {
                val reminder = Reminder(key, latlng.latitude, latlng.longitude)
                reference.child(key).setValue(reminder)
            }
*/
//            createGeoFence(it, key!!, geofencingClient)
        }
    }

    private fun setReminderLocation(map: GoogleMap, location: LatLng) {
        val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Lng: %2$.5f",
                location.latitude,
                location.longitude
        )
        if(this::reminderMarker.isInitialized) reminderMarker.remove()
        reminderMarker = map.addMarker(
                MarkerOptions()
                        .position(location)
                        .title(getString(R.string.dropped_pin))
                        .snippet(snippet)
        )
        if(this::reminderArea.isInitialized) reminderArea.remove()
        reminderArea = map.addCircle(
                CircleOptions()
                        .center(location)
                        .strokeColor(Color.argb(50, 70, 70, 70))
                        .fillColor(Color.argb(70, 150, 150, 150))
                        .radius(GEOFENCE_RADIUS.toDouble())
        )
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, CAMERA_ZOOM_LEVEL))
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(MarkerOptions()
                .position(poi.latLng)
                .title(poi.name)
            )
            poiMarker.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            )
            if (!success) {
                Log.e("hw_project", "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("hw_project", "Can't find style. Error: ", e)
        }
    }

    private fun createGeoFence(location: LatLng, key: String) {
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(location.latitude, location.longitude, GEOFENCE_RADIUS.toFloat())
            .setExpirationDuration(GEOFENCE_EXPIRATION.toLong())
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(GEOFENCE_DWELL_DELAY)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
/*
        val intent = Intent(this, GeofenceReceiver::class.java)
            .putExtra("key", key)
            .putExtra("message", "Geofence alert - ${location.latitude}, ${location.longitude}")

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    GEOFENCE_LOCATION_REQUEST_CODE
                )
            } else {
                //map.geofencingClient.addGeofences(geofenceRequest, pendingIntent)
            }
        } else {
            //map.geofencingClient.addGeofences(geofenceRequest, pendingIntent)
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true
        } else {
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && (
                        grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                        grantResults[1] == PackageManager.PERMISSION_GRANTED)
                ) {
                    // Permission from popup granted
                    enableMyLocation()
                } else {
                    // Permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            GEOFENCE_LOCATION_REQUEST_CODE -> {
                if (permissions.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "This application needs background location to work on Android 10 and higher",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        fun removeGeofences(context: Context, triggeringGeofenceList: MutableList<Geofence>) {
            val geofenceIdList = mutableListOf<String>()
            for (entry in triggeringGeofenceList) {
                geofenceIdList.add(entry.requestId)
            }
            LocationServices.getGeofencingClient(context).removeGeofences(geofenceIdList)
        }
    }
}