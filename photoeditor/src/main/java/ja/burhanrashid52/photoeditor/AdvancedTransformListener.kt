package ja.burhanrashid52.photoeditor

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import ja.burhanrashid52.photoeditor.shape.ShapeView
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("ClickableViewAccessibility")
class AdvancedTransformListener(
    private val viewToTransform: View,
    private val handleType: HandleType,
//    private val allHandles: List<View>
) : View.OnTouchListener {

    enum class HandleType {
        ROTATE,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP, BOTTOM, LEFT, RIGHT
    }

    private var lastX = 0f
    private var lastY = 0f

    // Untuk rotasi
    private var pivotX = 0f
    private var pivotY = 0f

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val graphic = (viewToTransform.tag as? Pair<*, *>)?.second as? Graphic ?: return true
        if (handleType == HandleType.ROTATE) {
            Log.d("RotateDebug", "onTouch triggered for ROTATE handle! Action: ${event.action}")
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.parent.requestDisallowInterceptTouchEvent(true)

                lastX = event.rawX
                lastY = event.rawY

                val location = IntArray(2)
                viewToTransform.getLocationOnScreen(location)
                pivotX = location[0] + viewToTransform.width / 2f
                pivotY = location[1] + viewToTransform.height / 2f
            }

            MotionEvent.ACTION_MOVE -> {
                graphic.updateHandlesScale()

                val dx = event.rawX - lastX
                val dy = event.rawY - lastY

                when (handleType) {
                    HandleType.ROTATE -> handleRotation(event)
                    else -> handleResize(dx, dy)
                }
//                updateShapeViewStroke()

                lastX = event.rawX
                lastY = event.rawY
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun updateShapeViewStroke() {
        val shapeView = viewToTransform.findViewById<ShapeView>(R.id.shape_view)
        if (shapeView != null) {
            val avgScale = (viewToTransform.scaleX + viewToTransform.scaleY) / 2f
            shapeView.setParentScale(avgScale)
        }
    }
    private fun handleRotation(event: MotionEvent) {
        val vector = Vector2D()
        vector.set(event.rawX - pivotX, event.rawY - pivotY)
        val newAngle = Math.toDegrees(atan2(vector.y.toDouble(), vector.x.toDouble())).toFloat()

        vector.set(lastX - pivotX, lastY - pivotY)
        val oldAngle = Math.toDegrees(atan2(vector.y.toDouble(), vector.x.toDouble())).toFloat()

        val rotation = newAngle - oldAngle
        viewToTransform.rotation += rotation
    }

    private fun handleResize(deltaX: Float, deltaY: Float) {
        val currentWidth = viewToTransform.width * viewToTransform.scaleX
        val currentHeight = viewToTransform.height * viewToTransform.scaleY

        var widthChange = 0f
        var heightChange = 0f

        val angleRad = Math.toRadians(viewToTransform.rotation.toDouble())
        val cosVal = cos(angleRad).toFloat()
        val sinVal = sin(angleRad).toFloat()

        val localDeltaX = deltaX * cosVal + deltaY * sinVal
        val localDeltaY = -deltaX * sinVal + deltaY * cosVal

        when (handleType) {
            HandleType.TOP_LEFT     -> { widthChange = -localDeltaX; heightChange = -localDeltaY }
            HandleType.TOP_RIGHT    -> { widthChange =  localDeltaX; heightChange = -localDeltaY }
            HandleType.BOTTOM_LEFT  -> { widthChange = -localDeltaX; heightChange =  localDeltaY }
            HandleType.BOTTOM_RIGHT -> { widthChange =  localDeltaX; heightChange =  localDeltaY }
            HandleType.TOP          -> { heightChange = -localDeltaY }
            HandleType.BOTTOM       -> { heightChange =  localDeltaY }
            HandleType.LEFT         -> { widthChange = -localDeltaX }
            HandleType.RIGHT        -> { widthChange =  localDeltaX }
            else -> return
        }

        val newWidth = currentWidth + widthChange
        val newHeight = currentHeight + heightChange

        val newScaleX = newWidth / viewToTransform.width
        val newScaleY = newHeight / viewToTransform.height

        val oldScaleX = viewToTransform.scaleX
        val oldScaleY = viewToTransform.scaleY

        val viewType = (viewToTransform.tag as? Pair<*, *>)?.first as? ViewType
        val isUniformScale = (viewType == ViewType.IMAGE || viewType == ViewType.TEXT)

        if (isUniformScale) {
            val oldScaleAvg = (oldScaleX + oldScaleY) / 2f
            val candidateUp = maxOf(newScaleX, newScaleY)
            val candidateDown = minOf(newScaleX, newScaleY)
            var newUniformScale = if (candidateUp >= oldScaleAvg) candidateUp else candidateDown
            if (newUniformScale < 0.1f) newUniformScale = 0.1f

            viewToTransform.scaleX = newUniformScale
            viewToTransform.scaleY = newUniformScale

            val moveX = (viewToTransform.width * (newUniformScale - oldScaleAvg)) / 2f
            val moveY = (viewToTransform.height * (newUniformScale - oldScaleAvg)) / 2f

            val pivotMoveX = when (handleType) {
                HandleType.TOP_LEFT, HandleType.BOTTOM_LEFT, HandleType.LEFT -> -moveX
                HandleType.TOP_RIGHT, HandleType.BOTTOM_RIGHT, HandleType.RIGHT -> moveX
                else -> 0f
            }
            val pivotMoveY = when (handleType) {
                HandleType.TOP_LEFT, HandleType.TOP_RIGHT, HandleType.TOP -> -moveY
                HandleType.BOTTOM_LEFT, HandleType.BOTTOM_RIGHT, HandleType.BOTTOM -> moveY
                else -> 0f
            }

            val rotatedMoveX = pivotMoveX * cosVal - pivotMoveY * sinVal
            val rotatedMoveY = pivotMoveX * sinVal + pivotMoveY * cosVal

            viewToTransform.translationX += rotatedMoveX
            viewToTransform.translationY += rotatedMoveY
        } else {
            if (newScaleX > 0.1f) {
                viewToTransform.scaleX = newScaleX
            }
            if (newScaleY > 0.1f) {
                viewToTransform.scaleY = newScaleY
            }

            val moveX = (viewToTransform.width * (newScaleX - oldScaleX)) / 2f
            val moveY = (viewToTransform.height * (newScaleY - oldScaleY)) / 2f

            val pivotMoveX = when (handleType) {
                HandleType.TOP_LEFT, HandleType.BOTTOM_LEFT, HandleType.LEFT -> -moveX
                HandleType.TOP_RIGHT, HandleType.BOTTOM_RIGHT, HandleType.RIGHT -> moveX
                else -> 0f
            }
            val pivotMoveY = when (handleType) {
                HandleType.TOP_LEFT, HandleType.TOP_RIGHT, HandleType.TOP -> -moveY
                HandleType.BOTTOM_LEFT, HandleType.BOTTOM_RIGHT, HandleType.BOTTOM -> moveY
                else -> 0f
            }

            val rotatedMoveX = pivotMoveX * cosVal - pivotMoveY * sinVal
            val rotatedMoveY = pivotMoveX * sinVal + pivotMoveY * cosVal

            viewToTransform.translationX += rotatedMoveX
            viewToTransform.translationY += rotatedMoveY
        }
    }
}
