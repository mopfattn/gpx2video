package de.pfattner.gpx2video.map.renderer.bitmap

import android.graphics.Bitmap
import android.graphics.Canvas
import de.pfattner.gpx2video.map.MapContent
import de.pfattner.gpx2video.map.MapType
import de.pfattner.gpx2video.map.MapViewLatLng
import de.pfattner.gpx2video.map.renderer.OffscreenMapRenderer
import java.io.IOException

class BitmapOffscreenMapRenderer(
    private val map: Bitmap?,
    mapType: MapType,
    width: Int,
    height: Int,
    density: Int,
    center: MapViewLatLng,
    zoom: Int,
    content: MapContent?,
) : OffscreenMapRenderer(mapType, width, height, density, center, zoom, content) {

    @Throws(IOException::class)
    override fun renderMap(canvas: Canvas) {
        map?.let {
            canvas.drawBitmap(map, 0f, 0f, null)
        }
    }
}
