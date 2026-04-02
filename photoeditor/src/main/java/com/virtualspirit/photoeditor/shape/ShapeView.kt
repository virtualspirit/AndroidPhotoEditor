
package com.virtualspirit.photoeditor.shape

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.virtualspirit.photoediting.StrokeStyle
import com.virtualspirit.photoeditor.shape.ShapeType

class ShapeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val fillPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
    }
    private var fillColor: Int? = null
    private var shapePath: Path? = null
    private var desiredStrokeWidth: Float = 0f
    private var parentScale = 1.0f // Default skala adalah 1

    private var currentStyle: StrokeStyle = StrokeStyle.SOLID
    private var currentShapeType: ShapeType = ShapeType.Brush

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setShape(builder: ShapeBuilder, path: Path) {
        this.shapePath = path
        this.desiredStrokeWidth = builder.shapeSize
        this.currentShapeType = builder.shapeType
        this.fillColor = builder.fillColor
        fillColor?.let { color ->
            fillPaint.color = color
            builder.shapeOpacity?.let { fillPaint.alpha = it }
        }
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
            paint.strokeWidth = builder.shapeSize
//            paint.strokeWidth = this.shapeSize
            paint.color = this.shapeColor
            this.shapeOpacity?.let { paint.alpha = it }
        }

        currentStyle = builder.shapeStyle
        applyPathEffect(paint.strokeWidth)
    }

    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        shapePath?.let {
//            Log.e("wew", "ShapeView.onDraw is executing. Drawing path with paint color: ${paint.color}")
//            canvas.drawPath(it, paint)
//        }
        super.onDraw(canvas)
        shapePath?.let { path ->
            // Draw fill first so stroke renders on top
            fillColor?.let { canvas.drawPath(path, fillPaint) }
            val currentScale = if (parentScale > 0) parentScale else 1.0f
            paint.strokeWidth = desiredStrokeWidth / currentScale
            applyPathEffect(paint.strokeWidth)
            canvas.drawPath(path, paint)
        }
    }

    fun updateColor(newColor: Int) {
        Log.e("wew", "ShapeView.updateColor called. Old paint color: ${paint.color}, New color: $newColor")
        paint.color = newColor
        invalidate()
    }

    fun getCurrentColor(): Int {
        return paint.color
    }

    fun updateStrokeWidth(newWidth: Float) {
        this.desiredStrokeWidth = newWidth
//        paint.strokeWidth = newWidth
        invalidate()
    }

    fun setParentScale(scale: Float) {
        if (this.parentScale != scale) {
            this.parentScale = scale
            invalidate()
        }
    }

    fun getCurrentStrokeWidth(): Float {
//        return paint.strokeWidth
        return this.desiredStrokeWidth
    }

    fun updateStrokeStyle(newStyle: StrokeStyle) {
        currentStyle = newStyle
        val currentScale = if (parentScale > 0) parentScale else 1.0f
        applyPathEffect(desiredStrokeWidth / currentScale)
        invalidate()
    }

    fun getCurrentStrokeStyle(): StrokeStyle {
        return currentStyle
    }

    fun getPath(): Path? {
        return shapePath
    }

    fun getCurrentFillColor(): Int? = fillColor

    fun updateFillColor(color: Int?) {
        fillColor = color
        if (color != null) {
            fillPaint.color = color
        }
        invalidate()
    }

    fun isClosedShape(): Boolean =
        currentShapeType == ShapeType.Oval || currentShapeType == ShapeType.Rectangle

    private fun applyPathEffect(strokeWidth: Float) {
        val w = strokeWidth.coerceAtLeast(1f)
        // With ROUND cap: visual dash = on + w, visual gap = off - w
        // 'on' must be proportional to w (not fixed) so dots stay circular at any scale.
        paint.pathEffect = when (currentStyle) {
            StrokeStyle.DASHED -> DashPathEffect(floatArrayOf(w, w * 2f), 0f)
            StrokeStyle.DOTTED -> DashPathEffect(floatArrayOf(w * 0.01f, w * 2f), 0f)
            StrokeStyle.SOLID -> null
        }
    }
}