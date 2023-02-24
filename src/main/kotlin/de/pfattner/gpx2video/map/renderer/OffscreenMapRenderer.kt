package de.pfattner.gpx2video.map.renderer

import android.graphics.*
import de.pfattner.gpx2video.map.MapContent
import de.pfattner.gpx2video.map.MapType
import de.pfattner.gpx2video.map.MapViewLatLng
import de.pfattner.gpx2video.map.MapViewPolyline
import de.pfattner.gpx2video.map.renderer.mapsforge.OnlineTileProvider
import org.mapsforge.core.graphics.TileBitmap
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.model.Tile
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.model.FixedTileSizeDisplayModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.min

@Suppress("PrivatePropertyName")
private val LOGGER = Logger.getLogger("OffscreenMapRenderer")

/**
 * Utility class for rendering an offscreen map.
 */
open class OffscreenMapRenderer(
    private val mapType: MapType,
    width: Int,
    height: Int,
    private val density: Int,
    private var center: MapViewLatLng,
    private var zoom: Int,
    private var content: MapContent? = null,
) {

    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    private val displayModel: DisplayModel
        get() {
            val tileSize = 256 * density
            val userScaleFactor = density / DisplayModel.getDeviceScaleFactor()

            val displayModel = FixedTileSizeDisplayModel(tileSize)
            displayModel.userScaleFactor = userScaleFactor
            return displayModel
        }

    private val tileProvider: TileProvider?
        get() {
            return when (mapType) {
                MapType.OSM -> {
                    val tileSource = OnlineTileProvider.getTileSource(mapType)
                    if (tileSource != null) OnlineTileProvider(tileSource) else null
                }

                else -> {
                    null
                }
            }
        }

    @Throws(IOException::class)
    fun build(): Bitmap {
        if (zoom < 0) {
            // use the content bounds for zooming
            val bounds = content?.bounds
            val boundingBox = if (bounds != null) {
                BoundingBox(
                    bounds.southWest.latitude,
                    bounds.southWest.longitude,
                    bounds.northEast.latitude,
                    bounds.northEast.longitude
                )
            } else {
                BoundingBox(-90.0, -180.0, 90.0, 180.0)
            }

            val dimension = Dimension((bitmap.width * 0.9).toInt(), (bitmap.height * 0.9).toInt())
            zoom = min(17, LatLongUtils.zoomForBounds(dimension, boundingBox, displayModel.tileSize).toInt())

            center = MapViewLatLng(
                boundingBox.minLatitude + (boundingBox.maxLatitude - boundingBox.minLatitude) / 2,
                boundingBox.minLongitude + (boundingBox.maxLongitude - boundingBox.minLongitude) / 2
            )
        }

        val canvas = Canvas(bitmap)

        // Try to render the map multiple times (due to possible exceptions when downloading map tiles)
        for (i in 0 until 2) {
            try {
                renderMap(canvas)
                break
            } catch (e: IOException) {
                LOGGER.log(Level.SEVERE, "Rendering map failed", e)
            }
        }

        content?.polylines?.forEach {
            renderPolylines(canvas, it)
        }

        return bitmap
    }

    @Throws(IOException::class)
    protected open fun renderMap(canvas: Canvas) {
        val width = bitmap.width
        val height = bitmap.height

        val displayModel = displayModel
        val tileProvider = tileProvider ?: return

        // Determine the center tile and check how many other tiles surrounding our center have to be rendered
        val centerTileX = MercatorProjection.longitudeToTileX(center.longitude, zoom.toByte())
        val centerTileY = MercatorProjection.latitudeToTileY(center.latitude, zoom.toByte())

        val centerTile = Tile(centerTileX, centerTileY, zoom.toByte(), displayModel.tileSize)

        // Determine the x/y pixel offset of the location within the tile
        val tileBoundary = centerTile.boundaryAbsolute
        val centerPixelXOffset = (MercatorProjection.longitudeToPixelX(
            center.longitude,
            zoom.toByte(),
            displayModel.tileSize
        ) - tileBoundary.left).toInt()
        val centerPixelYOffset = (MercatorProjection.latitudeToPixelY(
            center.latitude,
            zoom.toByte(),
            displayModel.tileSize
        ) - tileBoundary.top).toInt()

        val leftTileX = centerTileX - (width / 2 / displayModel.tileSize + 1)
        val rightTileX = centerTileX + (width / 2 / displayModel.tileSize + 1)
        val topTileY = centerTileY - (height / 2 / displayModel.tileSize + 1)
        val bottomTileY = centerTileY + (height / 2 / displayModel.tileSize + 1)

        val maxTileNumber = Tile.getMaxTileNumber(zoom.toByte())

        for (tileX in leftTileX..rightTileX) {
            if (tileX < 0 || tileX > maxTileNumber) {
                continue
            }
            for (tileY in topTileY..bottomTileY) {
                if (tileY < 0 || tileY > maxTileNumber) {
                    continue
                }
                val tile = Tile(tileX, tileY, zoom.toByte(), displayModel.tileSize)
                val bitmap = tileProvider.getTile(tile)
                val tileOffsetX = (centerTileX - tileX) * displayModel.tileSize
                val tileOffsetY = (centerTileY - tileY) * displayModel.tileSize

                val tileLeft = width / 2 - centerPixelXOffset - tileOffsetX
                val tileTop = height / 2 - centerPixelYOffset - tileOffsetY

                val src = Rect(0, 0, bitmap.width, bitmap.height)
                val dest = Rect(tileLeft, tileTop, tileLeft + displayModel.tileSize, tileTop + displayModel.tileSize)
                canvas.drawBitmap(bitmap, src, dest, null)
            }
        }

        tileProvider.destroy()
    }

    private fun renderPolylines(canvas: Canvas, polyline: MapViewPolyline) {
        val displayModel = displayModel

        val centerPixelX = MercatorProjection.longitudeToPixelX(center.longitude, zoom.toByte(), displayModel.tileSize)
        val centerPixelY = MercatorProjection.latitudeToPixelY(center.latitude, zoom.toByte(), displayModel.tileSize)

        val path = Path()

        polyline.points.forEach {
            val pointPixelX = MercatorProjection.longitudeToPixelX(it.longitude, zoom.toByte(), displayModel.tileSize)
            val pointPixelY = MercatorProjection.latitudeToPixelY(it.latitude, zoom.toByte(), displayModel.tileSize)

            val x = (pointPixelX - centerPixelX).toInt() + bitmap.width / 2
            val y = (pointPixelY - centerPixelY).toInt() + bitmap.height / 2

            if (path.isEmpty) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }

        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.isAntiAlias = true

        if (polyline.outlineWidth > 0 && polyline.outlineColor != 0) {
            paint.strokeWidth = polyline.outlineWidth
            paint.color = polyline.outlineColor
            canvas.drawPath(path, paint)
        }

        paint.strokeWidth = polyline.width
        paint.color = polyline.color
        canvas.drawPath(path, paint)
    }

    interface TileProvider {
        @Throws(IOException::class)
        fun getTile(tile: Tile): Bitmap

        fun destroy()
    }

    companion object {

        @Throws(IOException::class)
        fun toBitmap(tileBitmap: TileBitmap): Bitmap {
            val output = ByteArrayOutputStream()
            tileBitmap.compress(output)
            return BitmapFactory.decodeStream(ByteArrayInputStream(output.toByteArray()))
        }
    }
}
