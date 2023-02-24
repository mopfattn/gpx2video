package de.pfattner.gpx2video.map

import kotlin.math.max
import kotlin.math.min

/**
 * Coordinate bounds.
 */
data class MapViewLatLngBounds(
    val southWest: MapViewLatLng,
    val northEast: MapViewLatLng
) {
    fun extend(other: MapViewLatLngBounds): MapViewLatLngBounds {
        val minLatitude = min(southWest.latitude, other.southWest.latitude)
        val maxLatitude = max(northEast.latitude, other.northEast.latitude)
        val minLongitude = min(southWest.longitude, other.southWest.longitude)
        val maxLongitude = max(northEast.longitude, other.northEast.longitude)

        return MapViewLatLngBounds(MapViewLatLng(minLatitude, minLongitude), MapViewLatLng(maxLatitude, maxLongitude))
    }

    fun extend(position: MapViewLatLng): MapViewLatLngBounds {
        val minLatitude = min(southWest.latitude, position.latitude)
        val maxLatitude = max(northEast.latitude, position.latitude)
        val minLongitude = min(southWest.longitude, position.longitude)
        val maxLongitude = max(northEast.longitude, position.longitude)

        return MapViewLatLngBounds(MapViewLatLng(minLatitude, minLongitude), MapViewLatLng(maxLatitude, maxLongitude))
    }
}