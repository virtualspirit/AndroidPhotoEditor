//package ja.burhanrashid52.photoeditor.shape
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Paint
//import android.graphics.PorterDuff
//import android.graphics.PorterDuffXfermode
//import android.util.AttributeSet
//import android.view.View
//
///**
// * A custom view that renders a single shape based on a ShapeBuilder.
// * This view is designed to be a movable, scalable graphic on the PhotoEditor.
// */
//class ShapeView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : View(context, attrs, defStyleAttr) {
//
//    private var shapeBuilder: ShapeBuilder? = null
//    private val paint = Paint()
//    private var shape: AbstractShape? = null
//
//    init {
//        // Necessary for transparency and correct drawing
//        setLayerType(LAYER_TYPE_SOFTWARE, null)
//    }
//
//    fun setShapeBuilder(builder: ShapeBuilder) {
//        this.shapeBuilder = builder
//        setupPaint()
//        invalidate()
//    }
//
//    private fun setupPaint() {
//        paint.isAntiAlias = true
//        paint.isDither = true
//        paint.style = Paint.Style.STROKE
//        paint.strokeJoin = Paint.Join.ROUND
//        paint.strokeCap = Paint.Cap.ROUND
//        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
//
//        shapeBuilder?.apply {
//            paint.strokeWidth = this.shapeSize
//            paint.color = this.shapeColor
//            this.shapeOpacity?.let { paint.alpha = it }
//        }
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        shapeBuilder?.let { builder ->
//            // Create the shape path based on the view's dimensions
//            shape = createShapeFromBuilder(builder)
//            // Draw the shape on the canvas
//            shape?.draw(canvas, paint)
//        }
//    }
//
//    private fun createShapeFromBuilder(builder: ShapeBuilder): AbstractShape {
//        val createdShape: AbstractShape = when (val shapeType = builder.shapeType) {
//            ShapeType.Rectangle -> RectangleShape()
//            ShapeType.Oval -> OvalShape()
//            ShapeType.Line -> LineShape(context)
//            is ShapeType.Arrow -> LineShape(context, shapeType.pointerLocation)
//            // BrushShape is more complex as it follows a path,
//            // for now, we handle geometric shapes.
//            // A simple representation could be a line or nothing.
//            else -> RectangleShape() // Default or handle as needed
//        }
//
//        // Define the shape's path based on the view's bounds
//        // We use padding to ensure the stroke width doesn't get clipped
//        val padding = paint.strokeWidth / 2
//        createdShape.startShape(padding, padding)
//        createdShape.moveShape(width - padding, height - padding)
//        createdShape.stopShape()
//        return createdShape
//    }
//}

// burhanrashid52/photoeditor/shape/ShapeView.kt (VERSI BARU & BENAR)
package ja.burhanrashid52.photoeditor.shape

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class ShapeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private var shapePath: Path? = null

    init {
        // PENTING: Matikan hardware acceleration untuk View ini
        // agar stroke/goresan tidak terlihat "kotak" saat di-scale.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setShape(builder: ShapeBuilder, path: Path) {
        this.shapePath = path
        setupPaint(builder)
        invalidate()
    }

    private fun setupPaint(builder: ShapeBuilder) {
        paint.isAntiAlias = true
        paint.isDither = true
        paint.style = Paint.Style.STROKE // Selalu STROKE untuk outline
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        builder.apply {
            paint.strokeWidth = this.shapeSize
            paint.color = this.shapeColor
            this.shapeOpacity?.let { paint.alpha = it }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Cukup gambar path yang sudah diberikan.
        // PhotoEditorView (parent) akan menangani posisi dan skala.
        shapePath?.let {
            canvas.drawPath(it, paint)
        }
    }
}