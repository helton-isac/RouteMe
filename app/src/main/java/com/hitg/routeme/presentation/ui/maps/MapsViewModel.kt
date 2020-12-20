package com.hitg.routeme.presentation.ui.maps

import androidx.lifecycle.MutableLiveData
import com.directions.route.*
import com.google.android.gms.maps.model.LatLng
import com.hitg.routeme.presentation.viewmodel.BaseViewModel
import java.util.*

class MapsViewModel(
    private val mapsApiKey: String
) : BaseViewModel(), RoutingListener {

    val deviceLocation = MutableLiveData<LatLng>()
    val mapsState = MutableLiveData<MapsState<List<LatLng>>>()

    fun persistDeviceLocation(deviceLocation: LatLng) {
        this.deviceLocation.value = deviceLocation
    }

    fun findRouteTo(endPosition: LatLng?) {
        if (endPosition == null) {
            mapsState.value = MapsState.ErrorMessage(MapsMessageCode.ERROR_INVALID_LOCATION)
            return
        } else if (deviceLocation.value == null) {
            mapsState.value = MapsState.ErrorMessage(MapsMessageCode.ERROR_NO_START_LOCATION)
            return
        }
        val routing: Routing = Routing.Builder()
            .travelMode(AbstractRouting.TravelMode.DRIVING)
            .withListener(this)
            .alternativeRoutes(false)
            .waypoints(deviceLocation.value, endPosition)
            .key(mapsApiKey)
            .build()
        @Suppress("DEPRECATION")
        routing.execute()
    }

    override fun onRoutingFailure(e: RouteException) {
        mapsState.value = MapsState.Error(e)
    }

    override fun onRoutingStart() {
        mapsState.value = MapsState.Loading
    }

    override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
        for (i in route.indices) {
            if (i == shortestRouteIndex) {
                mapsState.value = MapsState.Success(route[shortestRouteIndex].points)
            }
        }
    }

    override fun onRoutingCancelled() {
        mapsState.value = MapsState.ErrorMessage(MapsMessageCode.ERROR_ROUTING_CANCELLED)
    }
}