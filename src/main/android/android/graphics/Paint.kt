package android.graphics

@Suppress("unused")
open class Paint {
    enum class Style(val nativeInt: Int) {
        FILL(0),
        STROKE(1),
        FILL_AND_STROKE(2),
    }

    enum class Cap(val nativeInt: Int) {
        BUTT(0),
        ROUND(1),
        SQUARE(2);
    }

    enum class Join(val nativeInt: Int) {
        MITER(0),
        ROUND(1),
        BEVEL(2);
    }

    var style = Style.STROKE
    var strokeCap = Cap.SQUARE
    var strokeJoin = Join.BEVEL
    var strokeWidth = 1f
    var color = 0
    var isAntiAlias = false
}