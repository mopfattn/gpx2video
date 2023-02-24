package android.graphics

import java.awt.BasicStroke
import java.awt.Color

open class Canvas(private val bitmap: Bitmap) {

    open fun drawBitmap(bitmap: Bitmap, src: Rect, dst: Rect, paint: Paint?) {
        this.bitmap.graphics.drawImage(
            bitmap.image,
            dst.left,
            dst.top,
            dst.right,
            dst.bottom,
            src.left,
            src.top,
            src.right,
            src.bottom,
            null
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
        this.bitmap.graphics.drawImage(bitmap.image, left.toInt(), top.toInt(), null)
    }

    fun drawPath(path: Path, paint: Paint) {
        val xValues = path.points.map { it.x.toInt() }.toIntArray()
        val yValues = path.points.map { it.y.toInt() }.toIntArray()

        val color = Color(paint.color, true)
        bitmap.graphics.apply {
            this.paint = color
            val cap = when (paint.strokeCap) {
                Paint.Cap.BUTT -> BasicStroke.CAP_BUTT
                Paint.Cap.ROUND -> BasicStroke.CAP_ROUND
                Paint.Cap.SQUARE -> BasicStroke.CAP_SQUARE
            }
            val join = when (paint.strokeJoin) {
                Paint.Join.MITER -> BasicStroke.JOIN_MITER
                Paint.Join.ROUND -> BasicStroke.JOIN_ROUND
                Paint.Join.BEVEL -> BasicStroke.JOIN_BEVEL
            }
            this.stroke = BasicStroke(paint.strokeWidth, cap, join)
            drawPolyline(xValues, yValues, xValues.size)
        }
    }
}