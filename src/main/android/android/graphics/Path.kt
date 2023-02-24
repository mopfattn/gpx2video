package android.graphics

import java.awt.geom.Point2D

class Path {
    val points = mutableListOf<Point2D.Float>()

    fun moveTo(x: Float, y: Float) {
        points.add(Point2D.Float(x, y))
    }

    fun lineTo(x: Float, y: Float) {
        points.add(Point2D.Float(x, y))
    }

    val isEmpty: Boolean get() = points.isEmpty()
}