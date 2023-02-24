package android.graphics

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.OutputStream
import javax.imageio.ImageIO


open class Bitmap(
    val image: BufferedImage,
) {
    enum class Config {
        ALPHA_8,
        RGB_565,
        ARGB_4444,
        ARGB_8888,
        RGBA_F16,
        HARDWARE,
        RGBA_1010102,
    }

    enum class CompressFormat(val nativeInt: Int) {
        JPEG(0),
        PNG(1),
        WEBP(2),
        WEBP_LOSSY(3),
        WEBP_LOSSLESS(4);
    }

    val graphics = image.createGraphics()
    val width = image.width
    val height = image.height

    constructor(width: Int, height: Int) : this(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB))

    init {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    }

    open fun compress(format: CompressFormat, quality: Int, stream: OutputStream?): Boolean {
        return when (format) {
            CompressFormat.JPEG -> ImageIO.write(image, "jpg", stream)
            CompressFormat.PNG -> ImageIO.write(image, "png", stream)
            CompressFormat.WEBP -> false
            CompressFormat.WEBP_LOSSY -> false
            CompressFormat.WEBP_LOSSLESS -> false
        }
    }

    companion object {
        @Suppress("UNUSED_PARAMETER")
        @JvmStatic
        fun createBitmap(width: Int, height: Int, config: Config): Bitmap {
            return Bitmap(width, height)
        }
    }
}