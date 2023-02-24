package de.pfattner.gpx2video

import de.pfattner.gpx2video.gpximporter.GpxImporter
import de.pfattner.gpx2video.gpximporter.GpxTrack
import de.pfattner.gpx2video.gpximporter.GpxTrackPoint
import de.pfattner.gpx2video.gpximporter.GpxTrackSegment
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import java.util.zip.ZipFile

@Suppress("PrivatePropertyName")
private val LOGGER = Logger.getLogger("TrackLoader")

/**
 * Loads tracks into memory, either from a file or a directory.
 */
class TrackLoader(private val options: Options) {
    data class Options(
        /**
         * Source file (zip file) or directory containing gpx files.
         */
        val sourceFile: File,

        /**
         * Filter for matching filenames, if filter is null all files are used.
         */
        val filenameFilter: Regex?
    )

    private fun matchesFilter(filename: String): Boolean {
        return options.filenameFilter == null || options.filenameFilter.matches(filename)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun loadTracks(): List<GpxTrack> {
        val result = mutableListOf<GpxTrack>()

        val count = AtomicInteger(0)

        if (options.sourceFile.isFile && options.sourceFile.name.endsWith(".zip", true)) {
            val zipFile = ZipFile(options.sourceFile)

            zipFile.use {
                val jobs = mutableListOf<Job>()

                LOGGER.info("Importing tracks from ${options.sourceFile}")
                zipFile.stream().forEach { entry ->
                    if (entry.name.endsWith(".gpx", ignoreCase = true) && matchesFilter(entry.name)) {
                        val job = GlobalScope.launch(Dispatchers.IO) {
                            zipFile.getInputStream(entry).use { input ->
                                loadTrack(entry.name, input)?.let {
                                    val currentCount = count.addAndGet(1)
                                    result.add(it)
                                    LOGGER.info("Loaded track #$currentCount: ${it.name} (${entry.name})")
                                }
                            }
                        }
                        jobs.add(job)
                    }
                }

                runBlocking {
                    jobs.joinAll()
                }
            }
        } else if (options.sourceFile.isDirectory) {
            val jobs = mutableListOf<Job>()

            // Find all GPX files within this directory
            LOGGER.info("Importing tracks from directory ${options.sourceFile}")
            options.sourceFile.listFiles(FileFilter { it.name.endsWith(".gpx", true) && matchesFilter(it.name) })?.forEach { file ->
                val job = GlobalScope.launch(Dispatchers.IO) {
                    FileInputStream(file).use { input ->
                        loadTrack(file.name, input)?.let {
                            val currentCount = count.addAndGet(1)
                            result.add(it)
                            LOGGER.info("Loaded track #$currentCount: ${it.name} (${file.name})")
                        }
                    }
                }
                jobs.add(job)
            }

            runBlocking {
                jobs.joinAll()
            }
        }

        return result
    }

    private fun loadTrack(filename: String, input: InputStream): GpxTrack? {
        val importer = GpxImporter()
        val track = importer.import(input)
        track.let {
            // Flatten the track points to keep only a single segment
            val trackPoints =
                track.trackSegments.flatMap { it.trackPoints }.sortedBy { it.timestampInMillis }
            if (trackPoints.isNotEmpty() && trackPoints.first().timestampInMillis != trackPoints.last().timestampInMillis) {
                return createTrack(trackPoints).apply {
                    name = track.name ?: filename
                }
            }
        }
        return null
    }

    /**
     * Creates a new track and starts all tracks at time 0.
     */
    private fun createTrack(trackPoints: List<GpxTrackPoint>): GpxTrack {
        val firstTime = trackPoints.first().timestampInMillis

        trackPoints.forEach {
            it.timestampInMillis -= firstTime
        }

        return GpxTrack().apply {
            trackSegments.add(GpxTrackSegment().apply {
                this.trackPoints.addAll(trackPoints)
            })
        }
    }
}