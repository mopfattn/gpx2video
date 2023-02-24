package de.pfattner.gpx2video.gpximporter

import de.pfattner.gpx2video.extensions.degreesToMicroDegrees
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@Suppress("PrivatePropertyName")
private val DATE_FORMATS = arrayOf<DateFormat>(
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
)

/**
 * Utility class for importing GPX files. See https://www.topografix.com/GPX/1/1/ for details.
 */
class GpxImporter {

    private enum class TrackType {
        TRACK, ROUTE
    }

    private val statusStack = Stack<String>()

    private val tracks = mutableListOf<GpxTrack>()
    private val trackTypes = mutableMapOf<GpxTrack, TrackType>()

    private val track: GpxTrack? get() = tracks.lastOrNull()
    private val trackSegment: GpxTrackSegment? get() = track?.trackSegments?.lastOrNull()

    private var gpxName = ""
    private var currentText = ""

    private var currentTrackPoint: GpxTrackPoint? = null

    fun import(input: InputStream): GpxTrack {
        val parser = KXmlParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, null)

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    handleStartTag(parser)
                }

                XmlPullParser.END_TAG -> {
                    handleEndTag(parser)
                }

                XmlPullParser.TEXT -> {
                    if (!parser.isWhitespace) {
                        handleText(parser)
                    }
                }
            }
        }

        tracks.forEach { track ->
            // Remove all empty track segments
            track.trackSegments = track.trackSegments.filter { it.trackPoints.isNotEmpty() }.toMutableList()
        }

        // Check whether we have tracks and routes, in this case only keep the tracks
        var tracks = trackTypes.filter { it.value == TrackType.TRACK }.map { it.key }
        val routes = trackTypes.filter { it.value == TrackType.ROUTE }.map { it.key }

        if (tracks.isEmpty()) {
            // Only use the routes if we have no tracks
            tracks = routes
        }

        // for now, merge all tracks into a single track using different segments
        val track = GpxTrack()
        tracks.filter { it.trackSegments.isNotEmpty() }.forEach {
            track.name = it.name ?: gpxName
            track.trackSegments.addAll(it.trackSegments)
        }

        return track
    }

    private fun handleStartTag(parser: XmlPullParser) {
        when (parser.namespace) {
            GpxConstants.GPXX_NAMESPACE -> handleGpxxStartTag(parser)
            else -> handleGpxStartTag(parser)
        }

        statusStack.push(parser.name)
    }

    private fun handleGpxxStartTag(parser: XmlPullParser) {
        when (parser.name) {
            GpxConstants.GPXX_RPT -> {
                if (statusStack.contains(GpxConstants.GPXX_ROUTE_POINT_EXTENSION)) {
                    track?.let {
                        val lat = parser.getAttributeValue(null, GpxConstants.LAT).toDoubleOrNull()
                        val lon = parser.getAttributeValue(null, GpxConstants.LON).toDoubleOrNull()
                        if (lat != null && lon != null) {
                            val trackPoint = GpxTrackPoint(
                                latitudeInMicroDegrees = lat.degreesToMicroDegrees(),
                                longitudeInMicroDegrees = lon.degreesToMicroDegrees()
                            )
                            trackSegment?.trackPoints?.add(trackPoint)
                        }
                    }
                }
            }
        }
    }

    private fun handleGpxStartTag(parser: XmlPullParser) {
        when (parser.name) {
            GpxConstants.TRK, GpxConstants.RTE -> {
                // Create a new track and track segment
                val track = GpxTrack()
                tracks.add(track)
                val trackSegment = GpxTrackSegment()
                track.trackSegments.add(trackSegment)

                if (parser.name == GpxConstants.RTE) {
                    trackTypes[track] = TrackType.ROUTE
                } else {
                    trackTypes[track] = TrackType.TRACK
                }
            }

            GpxConstants.TRKSEG -> {
                // Start a new track segment, if the previous segment is not empty
                if (trackSegment != null || trackSegment?.trackPoints?.isEmpty() == false) {
                    track?.let { track ->
                        val trackSegment = GpxTrackSegment()
                        track.trackSegments.add(trackSegment)
                    }
                }
            }

            GpxConstants.WPT -> {
                // Ignore waypoints
            }

            GpxConstants.TRKPT, GpxConstants.RTEPT -> {
                track?.let {
                    // Get lat/lon from the attributes
                    val lat = parser.getAttributeValue(null, GpxConstants.LAT).toDoubleOrNull()
                    val lon = parser.getAttributeValue(null, GpxConstants.LON).toDoubleOrNull()

                    if (lat != null && lon != null) {
                        val trackPoint = GpxTrackPoint(
                            latitudeInMicroDegrees = lat.degreesToMicroDegrees(),
                            longitudeInMicroDegrees = lon.degreesToMicroDegrees()
                        )
                        trackSegment?.trackPoints?.add(trackPoint)
                        currentTrackPoint = trackPoint
                    }
                }
            }
        }
    }

    private fun handleEndTag(parser: XmlPullParser) {
        when (parser.name) {
            GpxConstants.NAME -> {
                if (currentText.isNotEmpty()) {
                    if (statusStack.contains(GpxConstants.METADATA) && !statusStack.contains(GpxConstants.AUTHOR)) {
                        gpxName = currentText
                    } else if (statusStack.contains(GpxConstants.RTEPT) || statusStack.contains(GpxConstants.TRKPT)) {
                        // ignore names for rtept and trkpt elements
                    } else if (statusStack.contains(GpxConstants.RTE) || statusStack.contains(GpxConstants.TRK)) {
                        track?.let {
                            if (it.name == null || it.name?.isEmpty() == true) {
                                it.name = currentText
                            }
                        }
                    }
                }
            }

            GpxConstants.ELE -> {
                if (currentText.isNotEmpty()) {
                    currentTrackPoint?.altitudeInMeters = currentText.trim().toDouble().toInt()
                }
            }

            GpxConstants.WPT, GpxConstants.TRKPT, GpxConstants.RTEPT -> {
                currentTrackPoint = null
            }

            GpxConstants.TIME -> {
                if (currentText.isNotEmpty() && (statusStack.contains(GpxConstants.TRKPT) ||
                        statusStack.contains(GpxConstants.RTEPT) ||
                        statusStack.contains(GpxConstants.WPT))
                ) {
                    var date: Date? = null
                    for (dateFormat in DATE_FORMATS) {
                        try {
                            date = dateFormat.parse(currentText)
                            if (date != null) {
                                break
                            }
                        } catch (e: ParseException) {
                            // ignored
                        }
                    }

                    currentTrackPoint?.timestampInMillis = date?.time ?: 0L
                }
            }
        }

        currentText = ""
        if (statusStack.isNotEmpty()) {
            statusStack.pop()
        }
    }

    private fun handleText(parser: XmlPullParser) {
        currentText = parser.text.trim()
    }
}