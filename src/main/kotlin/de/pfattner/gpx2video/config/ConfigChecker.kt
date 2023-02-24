package de.pfattner.gpx2video.config

import java.io.File
import java.util.logging.Logger

private val LOGGER = Logger.getLogger("gpx2video")

object ConfigChecker {
    /**
     * Checks whether the config contains valid parameters.
     *
     * @return true upon success.
     */
    fun checkConfig(config: Config): Boolean {
        val inputFile = File(config.input)
        if (!inputFile.exists()) {
            LOGGER.severe("Input file/directory $inputFile not found")
            return false
        }

        if (inputFile.isFile && !inputFile.name.endsWith(".zip", true)) {
            LOGGER.severe("Input file $inputFile is not a zip file")
            return false
        }

        if (config.video.width <= 0 || config.video.height <= 0) {
            LOGGER.severe("Invalid video size: ${config.video.width} x ${config.video.height} (must be > 0)")
            return false
        }

        if (config.video.stepInSeconds <= 0) {
            LOGGER.severe("Invalid step: ${config.video.stepInSeconds} (must be > 0)")
            return false
        }

        if (config.video.maxDurationInSeconds <= 0) {
            LOGGER.severe("Invalid max duration: ${config.video.maxDurationInSeconds} (must be > 0)")
            return false
        }

        if (config.video.highlightDurationInSeconds <= 0) {
            LOGGER.severe("Invalid highlight duration: ${config.video.highlightDurationInSeconds} (must be > 0)")
            return false
        }

        if (config.video.highlightDurationInSeconds <= 0) {
            LOGGER.severe("Invalid highlight duration: ${config.video.highlightDurationInSeconds} (must be > 0)")
            return false
        }

        if (config.video.fadedWidth < 0) {
            LOGGER.severe("Invalid faded width duration: ${config.video.fadedWidth} (must be >= 0)")
            return false
        }

        if (config.video.highlightedWidth < 0) {
            LOGGER.severe("Invalid highlighted width duration: ${config.video.highlightedWidth} (must be >= 0)")
            return false
        }

        runCatching {
            ColorParser.parseColor(config.video.fadedColor)
        }.onFailure {
            LOGGER.severe("Invalid faded color: ${config.video.fadedColor} (expected argb hex value)")
            return false
        }

        runCatching {
            ColorParser.parseColor(config.video.highlightedColor)
        }.onFailure {
            LOGGER.severe("Invalid highlight color: ${config.video.highlightedColor} (expected argb hex value)")
            return false
        }

        if (!(config.map.zoom in 0..19)) {
            LOGGER.severe("Invalid zoom level: ${config.map.zoom} (must be in the range of [0..19])")
            return false
        }

        if (config.map.latitude < -85 || config.map.latitude > 85) {
            LOGGER.severe("Invalid latitude: ${config.map.latitude} (must be in the range of [-85..85])")
            return false
        }

        if (config.map.longitude < -180 || config.map.longitude > 180) {
            LOGGER.severe("Invalid latitude: ${config.map.latitude} (must be in the range of [-180..180])")
            return false
        }

        return true
    }
}