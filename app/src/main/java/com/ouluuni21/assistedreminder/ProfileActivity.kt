package com.ouluuni21.assistedreminder

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*


class ProfileActivity : AppCompatActivity() {

    private var mLocationManager: LocationManager? = null
    private var isRunning: Boolean = false
    private var mContext: Context? = null
    private var mRunnable: Runnable? = null
    private var mHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        mContext = applicationContext

        mLocationManager = mContext!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        showUser()
        initLocCoords()

        findViewById<Button>(R.id.logout).setOnClickListener {
            Log.d("hw_project", "Logout button clicked")
            if( logout() == 1) {
                this.startActivity(Intent(applicationContext, LoginActivity::class.java))
            }
        }

        findViewById<ImageView>(R.id.addMockMapLocation).setOnClickListener {
            this.startActivity(Intent(applicationContext, MapActivity::class.java))
        }

        val btnMockLocation = findViewById<Button>(R.id.btnMockLocation)
        btnMockLocation.setOnClickListener {
            mockLocation(it, btnMockLocation)
        }
    }

    private fun showUser() {
        val sharedPref = applicationContext.getSharedPreferences(
                getString(R.string.preference_file), MODE_PRIVATE
        )
        val currentUser = sharedPref.getString("Username", "")
        val user = findViewById<TextView>(R.id.profileName)
        user.text = currentUser
    }

    private fun logout(): Int {
        val sharedPref = applicationContext.getSharedPreferences(
                getString(R.string.preference_file), MODE_PRIVATE
        )
        sharedPref.edit().putInt("LoginStatus", 0).apply()
        return 1
    }

    private fun initLocCoords() {
        val sharedPref = applicationContext.getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE
        )

        sharedPref.edit().putFloat("latitude", 65.05993246473764F).apply()
        sharedPref.edit().putFloat("longitude", 25.467624998875436F).apply()

        stopMockLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        refreshView()
    }

    private fun refreshView() {
        val sharedPref = applicationContext.getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE
        )
        val latitude = sharedPref.getFloat("latitude", 0.0F).toDouble()
        val longitude = sharedPref.getFloat("longitude", 0.0F).toDouble()

        val locCoords = findViewById<TextView>(R.id.txtMockLocCoords)
        locCoords.text = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f\nLng: %2$.5f",
                latitude,
                longitude)
    }

    // https://betterprogramming.pub/how-to-fake-your-location-programmatically-and-avoid-being-tracked-by-companies-37447ec8f740
    private fun mockLocation(v: View, button: Button) {
        if (!isMockLocationEnabled()) {
            Toast.makeText(v.getContext(), "Please turn on Mock Location permission on Developer Settings", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            return
        }
        if (isRunning) {
            stopMockLocationUpdates()
            button.setText("Start")
        } else {
            // Very quick and dirty way to get latest marker info
            val sharedPref = applicationContext.getSharedPreferences(
                    getString(R.string.preference_file), Context.MODE_PRIVATE
            )
            val latitude = sharedPref.getFloat("latitude", 0.0F).toDouble()
            val longitude = sharedPref.getFloat("longitude", 0.0F).toDouble()

            startMockLocationUpdates(latitude, longitude)
            button.setText("Stop")
        }
        isRunning = !isRunning
    }

    private fun isMockLocationEnabled(): Boolean {
        val isMockLocation: Boolean
        isMockLocation = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val opsManager = mContext!!.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                Objects.requireNonNull(opsManager).checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), BuildConfig.APPLICATION_ID) === AppOpsManager.MODE_ALLOWED
            } else {
                Settings.Secure.getString(mContext!!.getContentResolver(), "mock_location") != "0"
            }
        } catch (e: Exception) {
            Log.d("hw_project", "Mock location is not enabled.");
            return false
        }
        return isMockLocation
    }

    fun startMockLocationUpdates(latitude: Double, longitude: Double) {
        mRunnable = Runnable {
            Log.w("hw_project", "startMockLocationUpdates for lat:$latitude lng:$longitude");
            setMock(LocationManager.GPS_PROVIDER, latitude, longitude)
            setMock(LocationManager.NETWORK_PROVIDER, latitude, longitude)
            mHandler!!.postDelayed(mRunnable, 500)
        }
        mHandler!!.post(mRunnable)
    }

    fun stopMockLocationUpdates() {
        Log.w("hw_project", "stopMockLocationUpdates");
        mHandler!!.removeCallbacks(mRunnable)
        try {
            mLocationManager!!.removeTestProvider(LocationManager.GPS_PROVIDER)
            mLocationManager!!.removeTestProvider(LocationManager.NETWORK_PROVIDER)
        }  catch (e: IllegalArgumentException) {
            Log.w("hw_project", "stopMockLocationUpdates" + e.message);
        }
    }

    private fun setMock(provider: String, latitude: Double, longitude: Double) {
        try {
           mLocationManager!!.addTestProvider(provider,
                   false,
                   false,
                   false,
                   false,
                   false,
                   true,
                   true,
                   0,
                   5)
        } catch (e: SecurityException) {
            Log.w("hw_project", "addTestProvider" + e.message);
        }
        try {
            mLocationManager!!.setTestProviderEnabled(provider, true)
        } catch (e: SecurityException) {
            Log.w("hw_project", "setTestProviderEnabled" + e.message);
        }

        val newLocation = Location(provider)
        newLocation.latitude = latitude
        newLocation.longitude = longitude
        newLocation.altitude = 3.0
        newLocation.time = System.currentTimeMillis()
        newLocation.speed = 0.01F
        newLocation.bearing = 1F
        newLocation.accuracy = 3F
        newLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            newLocation.bearingAccuracyDegrees = 0.1F
            newLocation.verticalAccuracyMeters = 0.1F
            newLocation.speedAccuracyMetersPerSecond = 0.01F
        }
        try {
            //mLocationManager!!.requestLocationUpdates(provider, 0, 0, this);
            mLocationManager!!.setTestProviderLocation(provider, newLocation)
        } catch (e: SecurityException) {
            Log.w("hw_project", "setTestProviderEnabled" + e.message);
            e.printStackTrace()
        }
    }
}