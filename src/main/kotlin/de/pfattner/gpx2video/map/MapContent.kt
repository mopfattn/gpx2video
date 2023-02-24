package de.pfattner.gpx2video.map

import android.content.Context
import de.pfattner.gpx2video.gpximporter.GpxTrack

/**
 * Holds the content for a map.
 */
@Suppress("DataClassPrivateConstructor")
data class MapContent private constructor(
    val context: Context,
    val tracks: List<GpxTrack>,
    val polylines: List<MapViewPolyline>,
    val bounds: MapViewLatLngBounds?
) {
    class Builder(private val context: Context) {

        private var bounds: MapViewLatLngBounds? = null
        private val tracks = mutableListOf<GpxTrack>()
        private val polylines = mutableListOf<MapViewPolyline>()

        fun addTrack(track: GpxTrack, color: Int? = null, drawDirectionArrows: Boolean = false): Builder {
            if (tracks.contains(track)) {
                return this
            }
            tracks.add(track)
            addPolylines(MapViewPolyline.fromTrack(track), color, drawDirectionArrows)

            return this
        }

        private fun addPolylines(
            polylines: List<MapViewPolyline>,
            color: Int? = null,
            drawDirectionArrows: Boolean = false
        ): Builder {
            polylines.forEach { addPolyline(it, color, drawDirectionArrows) }
            return this
        }

        private fun addPolyline(
            polyline: MapViewPolyline,
            color: Int? = null,
            drawDirectionArrows: Boolean = false
        ): Builder {
            polyline.let {
                it.color = color ?: 0
                it.width = 1f
                it.outlineWidth = 0f
                it.directionArrows = drawDirectionArrows

                it.bounds?.let { bounds -> this.bounds = (this.bounds ?: bounds).extend(bounds) }
            }
            this.polylines.add(polyline)

            return this
        }

        fun build(): MapContent {
            return MapContent(context, ArrayList(tracks), ArrayList(polylines), bounds?.copy())
        }

    }
}