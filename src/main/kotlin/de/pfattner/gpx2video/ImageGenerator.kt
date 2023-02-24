package de.pfattner.gpx2video

import android.content.Context
import android.graphics.Bitmap
import de.pfattner.gpx2video.gpximporter.GpxTrack
import de.pfattner.gpx2video.gpximporter.GpxTrackSegment
import de.pfattner.gpx2video.map.MapContent
import de.pfattner.gpx2video.map.MapType
import de.pfattner.gpx2video.map.MapViewLatLng
import de.pfattner.gpx2video.map.renderer.OffscreenMap
import kotlinx.coroutines.*
import java.awt.image.BufferedImage
import java.awt.image.LookupOp
import java.awt.image.ShortLookupTable
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Suppress("PrivatePropertyName")
private val LOGGER = Logger.getLogger("ImageGenerator")

private const val MAP_FILE_PATTERN = "map_%05d.png"

/**
 * Generates single images for track points that can later be converted into a video.
 */
class ImageGenerator(private val options: Options) {
    data class Options(
        /**
         * Image size
         */
        val size: Size,
        /**
         * Map center
         */
        val center: MapViewLatLng,
        /**
         * Map zoom level [0-19] (0 = whole world)
         */
        val zoomLevel: Int,
        /**
         * Output directory where to save the images
         */
        val outputDirectory: File,
        /**
         * Command to invoke after the images have been generated
         */
        val runCommand: String?,
        /**
         * Maximum duration of the track
         */
        val maxTrackDuration: Duration = 6.hours,
        /**
         * Steps to forward
         */
        val stepForwardBy: Duration = 1.minutes,
        /**
         * Part of the track to highlight
         */
        val highlightDuration: Duration = 5.minutes,
        /**
         * Theme to use for styling the tracks and map
         */
        val theme: Theme,
        /**
         * Delete generated images after running the command
         */
        val deleteGenerateImages: Boolean = false,
    )

    data class Size(
        /**
         * Image width
         */
        val width: Int,
        /**
         * Image height
         */
        val height: Int,
        /**
         * Width of the highlighted line
         */
        val trackHighlightWidth: Float,
        /**
         * Width of the faded out (old) line
         */
        val trackFadedWidth: Float,
    )

    @Suppress("unused")
    data class Theme(
        val trackColor: Int = 0xFFFF00FFL.toInt(),
        val trackFadedColor: Int = 0x40FFA500,
        val darkMap: Boolean = false,
    ) {
        companion object {
            val DEFAULT = Theme(
                trackColor = 0xFF0077FFL.toInt(),
                trackFadedColor = 0x400040A0,
                darkMap = true,
            )
        }
    }

    private fun createSegments(
        tracks: List<GpxTrack>,
        from: Int,
        to: Int,
    ): List<GpxTrack> {
        val result = mutableListOf<GpxTrack>()
        tracks.forEach { track ->
            val filteredTrackPoints =
                track.trackSegments.first().trackPoints.filter { it.timestampInMillis in from until to }
            val filteredTrack = GpxTrack()
            filteredTrack.trackSegments.add(GpxTrackSegment())
            filteredTrack.trackSegments.first().trackPoints.addAll(filteredTrackPoints)

            if (filteredTrack.trackSegments.first().trackPoints.isNotEmpty()) {
                result.add(filteredTrack)
            }
        }
        return result
    }

    private fun Bitmap.toGrayScale(): Bitmap {
        val gray = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        gray.graphics.drawImage(image, 0, 0, null)
        return Bitmap(gray)
    }

    private fun Bitmap.invertColors(): Bitmap {
        val inverted = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        val invertTable = ShortArray(256) { (255 - it).toShort() }
        val invertOp = LookupOp(ShortLookupTable(0, invertTable), null)
        invertOp.filter(image, inverted)
        return Bitmap(inverted)
    }

    private fun getFile(frame: Int): File {
        return File(options.outputDirectory, String.format(MAP_FILE_PATTERN, frame))
    }

    private fun Bitmap.writeToFile(frame: Int): File {
        val file = getFile(frame)
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        FileOutputStream(file).use {
            compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file
    }

    private fun maxTimestamp(tracks: List<GpxTrack>): Int {
        return tracks.maxOf { it.trackSegments.last().trackPoints.last().timestampInMillis }.toInt()
    }

    @Suppress("MemberVisibilityCanBePrivate", "OPT_IN_USAGE")
    private fun generateImages(tracks: List<GpxTrack>): List<File> {
        val context = Context()
        val files = CopyOnWriteArrayList<File>()

        // Delete all images in the output directory
        LOGGER.info("Deleting all images from ${options.outputDirectory}")
        options.outputDirectory.listFiles(FileFilter { it.name.endsWith(".png", true) })?.forEach { it.delete() }

        val maxTime = min(maxTimestamp(tracks), options.maxTrackDuration.inWholeMilliseconds.toInt())
        val numFrames = (maxTime + options.highlightDuration.inWholeMilliseconds.toInt()) / options.stepForwardBy.inWholeMilliseconds.toInt()
        LOGGER.info("Generating $numFrames frames ...")

        // First generate the background map to only download the tiles once
        val builder = OffscreenMap(context)
            .setWidth(options.size.width)
            .setHeight(options.size.height)
            .setDensity(2)
            .setMapType(MapType.OSM)
            .setCenter(options.center)
            .setZoom(options.zoomLevel)

        var background = builder.build()
        background = background.toGrayScale()
        if (options.theme.darkMap) {
            background = background.invertColors()
        }

        files.add(background.writeToFile(0))

        var frameCounter = 0

        builder.setMapType(MapType.NO_MAP)

        val jobs = mutableListOf<Job>()

        var oldPercent = 0

        while (true) {
            val to = (frameCounter + 1) * options.stepForwardBy.inWholeMilliseconds.toInt()
            val from = to - options.highlightDuration.inWholeMilliseconds.toInt()

            if (from >= options.maxTrackDuration.inWholeMilliseconds) {
                break
            }

            val start = System.currentTimeMillis()
            val trackSegments = createSegments(tracks, from, to)
            val end = System.currentTimeMillis()
            if (trackSegments.isEmpty()) {
                break
            }

            ++frameCounter
            val currentFrameCounter = frameCounter

            LOGGER.fine("Frame #$frameCounter (loaded ${trackSegments.size} tracks in (${end - start} ms) ...")

            val mapContent = MapContent.Builder(context)
                .apply {
                    trackSegments.forEach { addTrack(it) }
                }
                .build()
            mapContent.polylines.forEach {
                it.outlineWidth = 0f
                it.width = options.size.trackHighlightWidth
                it.color = options.theme.trackColor
            }

            val bitmap = builder
                .setBackground(background)
                .setContent(mapContent)
                .build()

            val percent = (frameCounter * 100 / numFrames).coerceIn(0..100)
            if (oldPercent != percent) {
                oldPercent = percent
                LOGGER.info("Generating frame: $percent% ($frameCounter frames generated)")
            }

            // Writing images takes a lot of cpu time, use a background thread instead
            val job = GlobalScope.launch(Dispatchers.IO) {
                files.add(bitmap.writeToFile(currentFrameCounter))
            }
            jobs.add(job)

            // Modify the content to draw thinner polylines and use the new content as background
            mapContent.polylines.forEach {
                it.outlineWidth = 0f
                it.width = options.size.trackFadedWidth
                it.color = options.theme.trackFadedColor
            }
            background = builder
                .setBackground(background)
                .setContent(mapContent)
                .build()
        }

        LOGGER.info("Waiting for image generator ...")

        runBlocking {
            jobs.joinAll()
        }

        return files
    }

    fun generateImagesAndRunCommand(tracks: List<GpxTrack>) {
        /**
         * Delete all existing images
         */
        options.outputDirectory.listFiles { file, _ -> file.name.endsWith(".png") }?.forEach { it.delete() }

        val files = generateImages(tracks)

        if (options.runCommand.isNullOrBlank()) {
            return
        }
        val cmd = options.runCommand.split(" ").map {
            it
                .replace("\${width}", "${options.size.width}")
                .replace("\${height}", "${options.size.height}")
                .replace("\${tmpDirectory}", "${options.outputDirectory}")
        }

        LOGGER.info("Running ${cmd.joinToString(" ")} ...")

        val process = Runtime.getRuntime().exec(cmd.toTypedArray())

        val errorThread = thread {
            process.errorStream.copyTo(System.out)
        }
        val outputThread = thread {
            process.inputStream.copyTo(System.out)
        }

        val result = process.waitFor()
        errorThread.join()
        outputThread.join()

        if (options.deleteGenerateImages) {
            files.forEach { it.delete() }
        }

        LOGGER.info("result: $result")
    }
}