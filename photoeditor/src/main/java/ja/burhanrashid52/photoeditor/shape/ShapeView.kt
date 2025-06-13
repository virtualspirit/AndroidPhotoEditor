
package ja.burhanrashid52.photoeditor.shape

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.View
import ja.burhanrashid52.photoediting.StrokeStyle

class ShapeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private var shapePath: Path? = null

    private var currentStyle: StrokeStyle = StrokeStyle.SOLID

    init {
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

        currentStyle = builder.shapeStyle
        applyPathEffect()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        shapePath?.let {
            Log.e("wew", "ShapeView.onDraw is executing. Drawing path with paint color: ${paint.color}")
            canvas.drawPath(it, paint)
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
        paint.strokeWidth = newWidth
        invalidate()
    }

    fun getCurrentStrokeWidth(): Float {
        return paint.strokeWidth
    }

    fun updateStrokeStyle(newStyle: StrokeStyle) {
        currentStyle = newStyle
        applyPathEffect()
        invalidate()
    }

    fun getCurrentStrokeStyle(): StrokeStyle {
        return currentStyle
    }

    fun getPath(): Path? {
        return shapePath
    }

    private fun applyPathEffect() {
        paint.pathEffect = when (currentStyle) {
            StrokeStyle.DASHED -> DashPathEffect(floatArrayOf(30f, 20f), 0f) // interval on, off
            StrokeStyle.DOTTED -> DashPathEffect(floatArrayOf(5f, 15f), 0f)  // interval on, off
            StrokeStyle.SOLID -> null // Hapus efek
        }
    }
}