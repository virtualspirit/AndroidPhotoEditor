package ja.burhanrashid52.photoeditor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.util.Log
import android.util.Pair
import android.view.MotionEvent
import android.view.View
import ja.burhanrashid52.photoeditor.shape.*
import java.util.*

/**
 *
 *
 * This is custom drawing view used to do painting on user touch events it it will paint on canvas
 * as per attributes provided to the paint
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 12/1/18
 */
class DrawingView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    private val drawShapes = Stack<ShapeAndPaint?>()
    private val redoShapes = Stack<ShapeAndPaint?>()
    internal var currentShape: ShapeAndPaint? = null
    var isDrawingEnabled = false
        private set
    private var viewChangeListener: BrushViewChangeListener? = null
    var currentShapeBuilder: ShapeBuilder

    var isShapeCreatingMode = false

    // endregion
    private fun createPaint(): Paint {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)

        // apply shape builder parameters
        currentShapeBuilder.apply {
            paint.strokeWidth = this.shapeSize
            // 'paint.color' must be called before 'paint.alpha',
            // otherwise 'paint.alpha' value will be overwritten.
            paint.color = this.shapeColor
            shapeOpacity?.also { paint.alpha = it }
        }

        return paint
    }

    fun clearAll() {
        drawShapes.clear()
        redoShapes.clear()
        invalidate()
    }

    fun setBrushViewChangeListener(brushViewChangeListener: BrushViewChangeListener?) {
        viewChangeListener = brushViewChangeListener
    }

    public override fun onDraw(canvas: Canvas) {
        for (shape in drawShapes) {
            shape?.shape?.draw(canvas, shape.paint)
        }
    }

    /**
     * Handle touch event to draw paint on canvas i.e brush drawing
     *
     * @param event points having touch info
     * @return true if handling touch events
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("DrawingView 1", "onTouchEvent: action=${event.action}, isDrawingEnabled=$isDrawingEnabled")
        // Hanya proses jika drawing diaktifkan
        if (!isDrawingEnabled) return false
        Log.d("DrawingView 2", "onTouchEvent: action=${event.action}, isDrawingEnabled=$isDrawingEnabled")

        val touchX = event.x
        val touchY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> onTouchEventDown(touchX, touchY)
            MotionEvent.ACTION_MOVE -> onTouchEventMove(touchX, touchY)
            MotionEvent.ACTION_UP -> onTouchEventUp(touchX, touchY) // Tidak perlu koordinat lagi
        }
        invalidate()
        return true
    }

    private fun onTouchEventDown(touchX: Float, touchY: Float) {
        createShape()
        currentShape?.shape?.startShape(touchX, touchY)
    }

    private fun onTouchEventMove(touchX: Float, touchY: Float) {
        currentShape?.shape?.moveShape(touchX, touchY)
    }

    private fun onTouchEventUp(touchX: Float, touchY: Float) {
        Log.e("DrawingView", "Shape hasBeenTapped: $touchX $touchY")
        val finalShape = currentShape?.shape
        Log.d("DrawingView", "onTouchEventUp: Shape is ${finalShape?.javaClass?.simpleName}")
        if (finalShape != null && !finalShape.hasBeenTapped()) {
            val tapped = finalShape.hasBeenTapped()
            Log.d("DrawingView", "Shape hasBeenTapped: $tapped")
            Log.d("DrawingView", "Shape bounds: ${finalShape.bounds}")
            if (isShapeCreatingMode) {
                Log.d("DrawingView", "onShapeCreated is being called.")
                viewChangeListener?.onShapeCreated(finalShape, touchX, touchY)
            } else {
                // Mode brush: selesaikan gambar
                finalShape.stopShape()
                viewChangeListener?.onStopDrawing()
                if (redoShapes.isNotEmpty()) {
                    redoShapes.clear()
                }
                viewChangeListener?.onViewAdd(this)
            }
        } else {
            Log.d("DrawingView", "onTouchEventUp: finalShape is null!")
        }
        // SELALU bersihkan path preview dari DrawingView setelah selesai
        drawShapes.clear()
        invalidate()
        currentShape = null
    }

    private fun createShape() {
        var paint = createPaint()
        var shape: AbstractShape = BrushShape()

        when (val shapeType = currentShapeBuilder.shapeType) {
            ShapeType.Oval -> {
                shape = OvalShape()
            }
            ShapeType.Brush -> {
                shape = BrushShape()
            }
            ShapeType.Rectangle -> {
                shape = RectangleShape()
            }
            ShapeType.Line -> {
                shape = LineShape(context)
            }
            is ShapeType.Arrow -> {
                shape = LineShape(context, shapeType.pointerLocation)
            }
        }

        currentShape = ShapeAndPaint(shape, paint)
        drawShapes.push(currentShape)
        viewChangeListener?.onStartDrawing()
    }

    fun undo(): Boolean {
        if (!drawShapes.empty()) {
            redoShapes.push(drawShapes.pop())
            invalidate()
        }
        viewChangeListener?.onViewRemoved(this)
        return !drawShapes.empty()
    }

    fun redo(): Boolean {
        if (!redoShapes.empty()) {
            drawShapes.push(redoShapes.pop())
            invalidate()
        }
        viewChangeListener?.onViewAdd(this)
        return !redoShapes.empty()
    }

    fun enableDrawing(brushDrawMode: Boolean) {
        isDrawingEnabled = brushDrawMode
        if (brushDrawMode) {
            visibility = VISIBLE
        } else {
            visibility = GONE
        }
    }

    // endregion
    val drawingPath: Pair<Stack<ShapeAndPaint?>, Stack<ShapeAndPaint?>>
        get() = Pair(drawShapes, redoShapes)

    // region constructors
    init {
        //Caution: This line is to disable hardware acceleration to make eraser feature work properly
        setLayerType(LAYER_TYPE_HARDWARE, null)
        visibility = GONE
        currentShapeBuilder = ShapeBuilder()
    }
}
