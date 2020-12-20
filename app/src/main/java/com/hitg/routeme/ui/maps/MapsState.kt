package com.hitg.routeme.ui.maps

sealed class MapsState<out T> {
    object Loading : MapsState<Nothing>()
    data class Success<T>(val data: T) : MapsState<T>()
    data class Error(val throwable: Throwable) : MapsState<Nothing>()
    data class ErrorMessage(val messageCode: MapsMessageCode) : MapsState<Nothing>()
}
