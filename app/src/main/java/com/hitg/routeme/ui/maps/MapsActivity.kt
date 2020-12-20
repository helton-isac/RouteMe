package com.hitg.routeme.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.directions.route.*
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.hitg.routeme.R
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, RoutingListener {

    private lateinit var map: GoogleMap
    private var cameraPosition: CameraPosition? = null

    private var locationPermissionGranted = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private val defaultLocation = LatLng(-23.550231183321824, -46.63392195099466)

    private var start: LatLng? = null
    private var end: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            val latitude = lastKnownLocation?.latitude
            val longitude = lastKnownLocation?.longitude
            if (latitude != null && longitude != null) {
                start = LatLng(latitude, longitude)
            }
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }

        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        Places.initialize(applicationContext, resources.getString(R.string.google_maps_key))
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initializeAutocompleteFragment() {
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment


        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )

        autocompleteFragment.setHint(resources.getString(R.string.search_place))

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                map.clear()
                val latLng = place.latLng

                if (latLng != null) {
                    end = latLng
                    val marker = MarkerOptions().position(latLng).title(place.name)
                    map.addMarker(marker)

                    val builder = LatLngBounds.Builder()

                    builder.include(marker.position)

                    val deviceLatitude = lastKnownLocation?.latitude
                    val deviceLongitude = lastKnownLocation?.longitude

                    if (deviceLatitude != null && deviceLongitude != null) {
                        builder.include(LatLng(deviceLatitude, deviceLongitude))
                    }
                    val bounds = builder.build()
                    val padding = 100
                    val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)

                    map.animateCamera(cu)
                    findRoutes(start, end)
                }
            }

            override fun onError(status: Status) {
                Log.i(TAG, "An error occurred: $status")
            }
        })


    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings?.isMyLocationButtonEnabled = false
        initializeAutocompleteFragment()
        if (getLocationPermission()) {
            updateLocationUI()
            getDeviceLocation()
        }
    }

    private fun updateLocationUI() {
        try {
            if (locationPermissionGranted) {
                map.isMyLocationEnabled = true
                getDeviceLocation()
            } else {
                map.isMyLocationEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            val latitude = lastKnownLocation?.latitude
                            val longitude = lastKnownLocation?.longitude
                            if (latitude != null && longitude != null) {
                                start = LatLng(latitude, longitude)
                            }
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
        return locationPermissionGranted
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {


                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
                }
            }
        }
        updateLocationUI()
    }

    fun findRoutes(Start: LatLng?, End: LatLng?) {
        if (Start == null || End == null) {
            Toast.makeText(this, "Unable to get location", Toast.LENGTH_LONG).show()
        } else {
            val routing: Routing = Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(Start, End)
                .key(resources.getString(R.string.google_maps_key))
                .build()
            routing.execute()
        }
    }

    companion object {
        private val TAG = MapsActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
    }

    override fun onRoutingFailure(e: RouteException) {
        Log.e(TAG, "onRoutingFailure: ${e.message}")
    }

    override fun onRoutingStart() {
        Log.i(TAG, "onRoutingStart")
    }

    private var polylines: MutableList<Polyline>? = null

    override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
        polylines?.clear()
        val builder = LatLngBounds.Builder()
        val polyOptions = PolylineOptions()
        polylines = mutableListOf()
        for (i in route.indices) {
            if (i == shortestRouteIndex) {
                polyOptions.color(ContextCompat.getColor(this, R.color.purple_700))
                polyOptions.width(7f)
                polyOptions.addAll(route[shortestRouteIndex].points)
                val polyline: Polyline = map.addPolyline(polyOptions)
                polylines?.add(polyline)

                for (point in route[shortestRouteIndex].points) {
                    builder.include(point)
                }
                val bounds = builder.build()
                val padding = 100
                val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                map.animateCamera(cu)
            }
        }
    }

    override fun onRoutingCancelled() {
        Log.i(TAG, "onRoutingCancelled")
    }
}