package de.pfattner.gpx2video.map.renderer.mapsforge

import android.graphics.Bitmap
import de.pfattner.gpx2video.map.MapType
import de.pfattner.gpx2video.map.renderer.OffscreenMapRenderer
import de.pfattner.gpx2video.map.renderer.mapsforge.OnlineTileProvider.TileDownloader
import org.mapsforge.core.graphics.CorruptedInputStreamException
import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.core.graphics.TileBitmap
import org.mapsforge.core.model.Tile
import org.mapsforge.core.util.IOUtils
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.download.DownloadJob
import org.mapsforge.map.layer.download.tilesource.OnlineTileSource
import org.mapsforge.map.layer.download.tilesource.TileSource
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URLConnection
import java.util.zip.GZIPInputStream

/**
 * A [OffscreenMapRenderer.TileProvider] that downloads tiles using a [TileDownloader].
 */
class OnlineTileProvider(private val tileSource: TileSource) : OffscreenMapRenderer.TileProvider {

    @Throws(IOException::class)
    override fun getTile(tile: Tile): Bitmap {
        val downloadJob = DownloadJob(tile, tileSource)
        val tileDownloader = TileDownloader(downloadJob, AndroidGraphicFactory.INSTANCE)
        val tileBitmap = tileDownloader.downloadImage()
        if (tileBitmap != null) {
            return OffscreenMapRenderer.toBitmap(tileBitmap)
        } else {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }

    override fun destroy() {}

    companion object {
        /**
         * Returns a [TileSource] for a specified map type.
         */
        fun getTileSource(mapType: MapType): TileSource? {
            return when (mapType) {
                MapType.OSM -> {
                    OnlineTileSource(
                        arrayOf(
                            "tile.openstreetmap.org"
                        ), 443
                    ).apply {
                        protocol = "https"
                        userAgent = "de.pfattner.gpx2video"
                    }
                }

                else -> null
            }
        }
    }

    private class TileDownloader(private val downloadJob: DownloadJob, private val graphicFactory: GraphicFactory) {

        @Throws(IOException::class)
        private fun getInputStream(urlConnection: URLConnection): InputStream {
            return if ("gzip" == urlConnection.contentEncoding) GZIPInputStream(urlConnection.getInputStream()) else urlConnection.getInputStream()
        }

        @Throws(IOException::class)
        fun downloadImage(): TileBitmap? {
            val url = this.downloadJob.tileSource.getTileUrl(this.downloadJob.tile)
            val urlConnection = url.openConnection()
            urlConnection.connectTimeout = this.downloadJob.tileSource.timeoutConnect
            urlConnection.readTimeout = this.downloadJob.tileSource.timeoutRead
            if (this.downloadJob.tileSource.userAgent != null) {
                urlConnection.setRequestProperty("User-Agent", this.downloadJob.tileSource.userAgent)
            }
            if (this.downloadJob.tileSource.referer != null) {
                urlConnection.setRequestProperty("Referer", this.downloadJob.tileSource.referer)
            }
            if (urlConnection is HttpURLConnection) {
                urlConnection.instanceFollowRedirects = this.downloadJob.tileSource.isFollowRedirects
            }
            val inputStream = getInputStream(urlConnection)

            return try {
                this.graphicFactory.createTileBitmap(
                    inputStream, this.downloadJob.tile.tileSize,
                    this.downloadJob.hasAlpha
                ).also { it.setExpiration(urlConnection.expiration) }
            } catch (e: CorruptedInputStreamException) {
                // the creation of the tile bitmap can fail, at least on Android,
                // when the connection is slow or busy, returning null here ensures that
                // the tile will be downloaded again
                null
            } finally {
                IOUtils.closeQuietly(inputStream)
            }
        }
    }
}
