package de.pfattner.gpx2video.config

object ColorParser {
    /**
     * Parses a hex string into a color value
     */
    fun parseColor(value: String?): Int? {
        val colorLong = if (value == null) {
            null
        } else if (value.startsWith("#")) {
            value.substring(1).toLong(16)
        } else if (value.startsWith("0x")) {
            value.substring(2).toLong(16)
        } else {
            null
        }
        return if (colorLong != null) {
            (colorLong and 0xFFFFFFFFL).toInt()
        } else {
            null
        }
    }
}