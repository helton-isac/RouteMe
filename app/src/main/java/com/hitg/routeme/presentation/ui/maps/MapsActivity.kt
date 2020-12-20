package com.hitg.routeme.presentation.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
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
import com.hitg.routeme.presentation.utils.DialogUtils
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapsViewModel: MapsViewModel
    private lateinit var map: GoogleMap
    private var locationPermissionGranted = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var polylines: MutableList<Polyline>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)

        initializePlaces()

        initializeFusedLocationProviderClient()

        initializeMap()

        initializeMyLocationButton()

        initializeViewModel()
    }

    private fun initializeViewModel() {

        mapsViewModel = ViewModelProvider(
            this,
            MapsViewModelFactory(resources.getString(R.string.google_maps_key))
        ).get(MapsViewModel::class.java)

        mapsViewModel.deviceLocation.observe(this, {
            hideLoading()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    it, DEFAULT_ZOOM.toFloat()
                )
            )
        })

        mapsViewModel.mapsState.observe(this, { mapsState ->
            hideLoading()
            when (mapsState) {
                is MapsState.Loading -> {
                    showLoading()
                }
                is MapsState.Error -> {
                    DialogUtils.showSimpleDialog(
                        this,
                        resources.getString(R.string.error),
                        mapsState.throwable.message ?: resources.getString(R.string.ops)
                    )
                }
                is MapsState.ErrorMessage -> {

                    if (mapsState.messageCode == MapsMessageCode.ERROR_NO_START_LOCATION) {
                        getLocationPermission()
                    } else {
                        DialogUtils.showSimpleDialog(
                            this,
                            resources.getString(R.string.error),
                            mapsState.messageCode.getTranslation(resources)
                        )
                    }
                }
                is MapsState.Success -> {
                    drawRoute(mapsState.data)
                }
            }
        })

    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun initializeFusedLocationProviderClient() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initializePlaces() {
        Places.initialize(applicationContext, resources.getString(R.string.google_maps_key))
    }

    private fun initializeMyLocationButton() {
        ivMyLocation.visibility = View.GONE
        ivMyLocation.setOnClickListener {
            getDeviceLocation()
        }
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initializeAutocompleteFragment() {
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )

        autocompleteFragment.setHint(resources.getString(R.string.search_place))

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                map.clear()
                val latLng = place.latLng

                if (latLng != null) {
                    val marker = MarkerOptions().position(latLng).title(place.name)
                    map.addMarker(marker)

                    val builder = LatLngBounds.Builder()
                    builder.include(marker.position)

                    val lastKnowLocation = mapsViewModel.deviceLocation.value
                    if (lastKnowLocation != null) {
                        builder.include(lastKnowLocation)
                    }

                    val bounds = builder.build()
                    val padding = 100
                    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

                    map.animateCamera(cameraUpdate)
                    mapsViewModel.findRouteTo(latLng)
                }
            }

            override fun onError(status: Status) {
                if (status != Status.RESULT_CANCELED) {
                    DialogUtils.showSimpleDialog(
                        this@MapsActivity,
                        resources.getString(R.string.error),
                        resources.getString(R.string.error_selecting_place) +
                                "$status"
                    )
                }
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings?.isMyLocationButtonEnabled = false
        initializeAutocompleteFragment()
        getLocationPermission()
    }

    private fun updateLocationUI() {
        try {
            if (locationPermissionGranted) {
                map.isMyLocationEnabled = true
                getDeviceLocation()
                ivMyLocation.visibility = View.VISIBLE
            } else {
                map.isMyLocationEnabled = false
            }
        } catch (e: SecurityException) {
            DialogUtils.showSimpleDialog(
                this,
                resources.getString(R.string.error),
                resources.getString(R.string.error_obtaining_current_location)
            )
        }
    }

    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val result = task.result
                        if (result != null) {
                            val latitude = result.latitude
                            val longitude = result.longitude
                            mapsViewModel.persistDeviceLocation(LatLng(latitude, longitude))
                        }
                    } else {
                        DialogUtils.showSimpleDialog(
                            this,
                            resources.getString(R.string.error),
                            resources.getString(R.string.error_obtaining_current_location)
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            DialogUtils.showSimpleDialog(
                this,
                resources.getString(R.string.error),
                resources.getString(R.string.error_obtaining_current_location)
            )
        }
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
            updateLocationUI()
        } else {
            locationPermissionGranted = false
            updateLocationUI()
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false

        hideLoading()

        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
                    updateLocationUI()
                } else {
                    DialogUtils.showSimpleDialog(
                        this, resources.getString(R.string.warning),
                        resources.getString(R.string.warning_needs_location_permission)
                    )
                }
            }
        }

    }

    private fun drawRoute(points: List<LatLng>) {
        polylines?.clear()
        val builder = LatLngBounds.Builder()
        val polyOptions = PolylineOptions()
        polylines = mutableListOf()

        polyOptions.color(ContextCompat.getColor(this, R.color.purple_700))
        polyOptions.width(7f)
        polyOptions.addAll(points)
        val polyline: Polyline = map.addPolyline(polyOptions)
        polylines?.add(polyline)

        for (point in points) {
            builder.include(point)
        }
        val bounds = builder.build()
        val padding = 100
        val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        map.animateCamera(cu)
    }

    companion object {
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }

}