package com.valdizz.locationnotification

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.valdizz.locationnotification.LocationUpdatesService.Companion.BROADCAST_REMOVE_GEOFENCE
import com.valdizz.locationnotification.LocationUpdatesService.Companion.EXTRA_STARTED_FROM_NOTIFICATION
import kotlinx.android.synthetic.main.activity_maps.*

/**
 * [MapsActivity] contains map and allows to enter the marked area by long pressing
 * or using dialog fragment (buttons [+] and [-]).
 *
 * @autor Vlad Kornev
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener, AddLocationFragment.OnButtonClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private var selectedLocationMarker: Marker? = null
    private var geofenceCircle: Circle? = null
    private var broadcastReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        btnAddGeofence.setOnClickListener {
            AddLocationFragment.newInstance().show(supportFragmentManager, GEOFENCE_ADD_DIALOG)
        }
        btnRemoveGeofence.setOnClickListener {
            removeGeofence()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        createBroadcastReceiver()
    }



    private fun createBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BROADCAST_REMOVE_GEOFENCE -> removeGeofence()
                }
            }
        }
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(broadcastReceiver as BroadcastReceiver, IntentFilter(BROADCAST_REMOVE_GEOFENCE))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(broadcastReceiver as BroadcastReceiver)
        saveCoordinatesToPreferences()
    }

    private fun getCoordinatesFromPreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (sharedPreferences.contains(LATITUDE) && sharedPreferences.contains(LONGITUDE)) {
            val latitude = sharedPreferences.getFloat(LATITUDE, 0f)
            val longitude = sharedPreferences.getFloat(LONGITUDE, 0f)
            markSelectedLocation(LatLng(latitude.toDouble(), longitude.toDouble()))
            btnRemoveGeofence.isVisible = true
        } else {
            btnRemoveGeofence.isVisible = false
        }
    }

    private fun saveCoordinatesToPreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (selectedLocationMarker != null) {
            sharedPreferences
                .edit()
                .putFloat(LATITUDE, selectedLocationMarker?.position?.latitude?.toFloat() ?: 0f)
                .putFloat(LONGITUDE, selectedLocationMarker?.position?.longitude?.toFloat() ?: 0f)
                .apply()
        } else {
            sharedPreferences.edit().clear().apply()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapLongClickListener(this)
        enableMyLocation()
        getCoordinatesFromPreferences()
    }

    override fun onMapLongClick(latLng: LatLng) {
        addGeofence(latLng)
    }

    override fun addGeofenceButtonClick(latLng: LatLng) {
        addGeofence(latLng)
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (checkPermissions()) {
            fusedLocationClient.lastLocation?.addOnCompleteListener {
                if (it.isSuccessful) {
                    map.isMyLocationEnabled = true
                    map.uiSettings.isMyLocationButtonEnabled = true
                    val location = LatLng(it.result!!.latitude, it.result!!.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM))
                } else {
                    map.isMyLocationEnabled = false
                    map.uiSettings.isMyLocationButtonEnabled = false
                }
            }
        } else {
            showSnackbar(getString(R.string.insufficient_permissions))
            requestPermissions(LOCATION_PERMISSION_MY_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(latLng: LatLng) {
        if (checkPermissions()) {
            geofencingClient.addGeofences(getGeofencingRequest(latLng), getGeofencePendingIntent())?.addOnCompleteListener{
                if (it.isSuccessful) {
                    markSelectedLocation(latLng)
                    startService(Intent(this, LocationUpdatesService::class.java))
                    btnRemoveGeofence.isVisible = true
                    showSnackbar(getString(R.string.geofence_added))
                } else {
                    showSnackbar(it.exception?.localizedMessage ?: getString(R.string.unknown_error))
                }
            }
        } else {
            showSnackbar(getString(R.string.insufficient_permissions))
            requestPermissions(LOCATION_PERMISSION_ADD_GEOFENCE)
        }
    }

    private fun removeGeofence() {
        if (checkPermissions()) {
            geofencingClient.removeGeofences(getGeofencePendingIntent())?.addOnCompleteListener {
                if (it.isSuccessful) {
                    selectedLocationMarker?.remove()
                    selectedLocationMarker = null
                    geofenceCircle?.remove()
                    stopService(Intent(this, LocationUpdatesService::class.java))
                    btnRemoveGeofence.isVisible = false
                    showSnackbar(getString(R.string.geofence_removed))
                } else {
                    showSnackbar(it.exception?.localizedMessage ?: getString(R.string.unknown_error))
                }
            }
        }
    }

    private fun markSelectedLocation(latLng: LatLng) {
        with (map) {
            selectedLocationMarker?.remove()
            selectedLocationMarker = addMarker(MarkerOptions().apply {
                position(latLng)
                title(getString(R.string.selected_marker_title, latLng.latitude.toString(), latLng.longitude.toString()))
            })
            drawGeofenceCircle(latLng)
        }
    }

    private fun drawGeofenceCircle(latLng: LatLng) {
        geofenceCircle?.remove()
        val circleOptions = CircleOptions().apply {
            center(latLng)
            strokeColor(Color.argb(60, 0, 0, 0))
            fillColor(Color.argb(40, 255, 0, 0))
            radius(GEOFENCE_RADIUS.toDouble())
        }
        geofenceCircle = map.addCircle(circleOptions)
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(this, LocationUpdatesService::class.java).apply {
            putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
        }
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getGeofencingRequest(latLng: LatLng): GeofencingRequest? {
        val geofence = Geofence.Builder().apply {
            setRequestId(GEOFENCE_ID)
            setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCE_RADIUS)
            setExpirationDuration(Geofence.NEVER_EXPIRE)
            setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        }.build()
        return GeofencingRequest.Builder().apply {
            setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
            addGeofence( geofence )
        }.build()
    }

    private fun showSnackbar(text: String) {
        val container = findViewById<View>(android.R.id.content)
        container?.let {
            Snackbar.make(it, text, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showSnackbar(mainTextStringId: Int, actionStringId: Int, listener: View.OnClickListener) {
        val container = findViewById<View>(android.R.id.content)
        container?.let {
            Snackbar.make(it, getString(mainTextStringId), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show()
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(requestCode: Int) {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (shouldProvideRationale) {
            showSnackbar(R.string.permission_rationale, android.R.string.ok, View.OnClickListener {
                ActivityCompat.requestPermissions(this@MapsActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
            })
        } else {
            ActivityCompat.requestPermissions(this@MapsActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode in arrayOf(LOCATION_PERMISSION_MY_LOCATION, LOCATION_PERMISSION_ADD_GEOFENCE)) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == LOCATION_PERMISSION_MY_LOCATION) {
                    enableMyLocation()
                }
            } else {
                showSnackbar(R.string.permission_denied_explanation, R.string.settings, View.OnClickListener {
                    val intent = Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                })
            }
        }
    }

    private companion object {
        private const val LOCATION_PERMISSION_MY_LOCATION = 1
        private const val LOCATION_PERMISSION_ADD_GEOFENCE = 2
        private const val GEOFENCE_ID = "Geofence"
        private const val GEOFENCE_RADIUS = 100f
        private const val DEFAULT_ZOOM = 15f
        private const val GEOFENCE_ADD_DIALOG = "AddGeofence"
        private const val LATITUDE = "latitude"
        private const val LONGITUDE = "longitude"
    }
}
