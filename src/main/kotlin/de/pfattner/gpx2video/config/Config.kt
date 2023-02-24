package de.pfattner.gpx2video.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("input")
    val input: String,

    @SerialName("tmpDir")
    val tmpDir: String? = null,

    @SerialName("video")
    val video: Video,

    @SerialName("map")
    val map: Map,

    @SerialName("runCommand")
    val runCommand: String? = null,
)

@Serializable
data class Map(
    @SerialName("latitude")
    val latitude: Double,

    @SerialName("longitude")
    val longitude: Double,

    @SerialName("zoom")
    val zoom: Int,

    @SerialName("invertColors")
    val invertColors: Boolean = false,
)

@Serializable
data class Video(
    @SerialName("maxDuration")
    val maxDurationInSeconds: Int,

    @SerialName("highlightDuration")
    val highlightDurationInSeconds: Int,

    @SerialName("step")
    val stepInSeconds: Int,

    @SerialName("width")
    val width: Int = 1920,

    @SerialName("height")
    val height: Int = 1080,

    @SerialName("fadedColor")
    val fadedColor: String? = null,

    @SerialName("fadedWidth")
    val fadedWidth: Int = 1,

    @SerialName("highlightedColor")
    val highlightedColor: String? = null,

    @SerialName("highlightedWidth")
    val highlightedWidth: Int = 1,
)