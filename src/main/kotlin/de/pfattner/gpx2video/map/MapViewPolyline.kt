package de.pfattner.gpx2video.map

import de.pfattner.gpx2video.gpximporter.GpxTrack
import de.pfattner.gpx2video.gpximporter.GpxTrackSegment

data class MapViewPolyline(
    val track: GpxTrack,
    var width: Float = 3f,
    var color: Int = 0xFFFF0000.toInt(),
    var outlineWidth: Float = 0f,
    var outlineColor: Int = 0,
    val points: List<MapViewLatLng>,
    var directionArrows: Boolean = false,
) {

    var bounds: MapViewLatLngBounds? = null

    init {
        val south = points.minOfOrNull { it.latitude }
        val north = points.maxOfOrNull { it.latitude }
        val west = points.minOfOrNull { it.longitude }
        val east = points.maxOfOrNull { it.longitude }

        if (south != null && north != null && west != null && east != null) {
            bounds = MapViewLatLngBounds(MapViewLatLng(south, west), MapViewLatLng(north, east))
        }
    }

    companion object {
        /**
         * Creates a [MapViewPolyline] with a [GpxTrackSegment].
         */
        private fun fromTrackSegment(track: GpxTrack, trackSegment: GpxTrackSegment): MapViewPolyline {
            val trackPoints = trackSegment.trackPoints
            return MapViewPolyline(track, points = trackPoints.map { MapViewLatLng(it) })
        }

        /**
         * Creates a list of [MapViewPolyline]s with a [GpxTrack], creating one [MapViewPolyline] for each [GpxTrackSegment].
         */
        fun fromTrack(track: GpxTrack): List<MapViewPolyline> {
            return track.trackSegments.map { fromTrackSegment(track, it) }
        }
    }
}