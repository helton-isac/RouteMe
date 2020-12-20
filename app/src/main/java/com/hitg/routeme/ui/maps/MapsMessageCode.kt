package com.hitg.routeme.ui.maps

import android.content.res.Resources
import com.hitg.routeme.R

enum class MapsMessageCode {
    ERROR_INVALID_LOCATION,
    ERROR_ROUTING_FAILURE,
    ERROR_ROUTING_CANCELLED;

    fun getTranslation(resources: Resources): String {
        return when (this) {
            ERROR_INVALID_LOCATION -> resources.getString(R.string.error_invalid_locations)
            ERROR_ROUTING_FAILURE -> resources.getString(R.string.error_routing_failure)
            ERROR_ROUTING_CANCELLED -> resources.getString(R.string.error_routing_cancelled)
        }
    }
}