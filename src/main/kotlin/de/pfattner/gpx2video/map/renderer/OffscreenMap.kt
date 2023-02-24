package de.pfattner.gpx2video.map.renderer

import android.content.Context
import android.graphics.Bitmap
import de.pfattner.gpx2video.map.MapContent
import de.pfattner.gpx2video.map.MapType
import de.pfattner.gpx2video.map.MapViewLatLng
import de.pfattner.gpx2video.map.renderer.bitmap.BitmapOffscreenMapRenderer
import java.io.IOException

/**
 * A map renderer used to render a [Bitmap].
 */
class OffscreenMap(private val context: Context) {
    private var width = 500
    private var height = 500
    private var center = MapViewLatLng(0.0, 0.0)
    private var zoom = -1
    private var density = 1
    private var mapType = MapType.NO_MAP
    private var background: Bitmap? = null

    private var content: MapContent? = null

    fun setWidth(width: Int): OffscreenMap {
        this.width = width
        return this
    }

    fun setHeight(height: Int): OffscreenMap {
        this.height = height
        return this
    }

    fun setCenter(center: MapViewLatLng): OffscreenMap {
        this.center = center
        return this
    }

    fun setZoom(zoom: Int): OffscreenMap {
        this.zoom = zoom
        return this
    }

    fun setDensity(density: Int): OffscreenMap {
        this.density = density
        return this
    }

    fun setMapType(mapType: MapType): OffscreenMap {
        this.mapType = mapType
        return this
    }

    fun setContent(mapContent: MapContent): OffscreenMap {
        this.content = mapContent
        return this
    }

    fun setBackground(background: Bitmap): OffscreenMap {
        this.background = background
        return this
    }

    @Throws(IOException::class)
    fun build(): Bitmap {
        val renderer: OffscreenMapRenderer = if (mapType == MapType.NO_MAP) {
            BitmapOffscreenMapRenderer(background, mapType, width, height, density, center, zoom, content)
        } else {
            OffscreenMapRenderer(mapType, width, height, density, center, zoom, content)
        }

        return renderer.build()
    }
}
