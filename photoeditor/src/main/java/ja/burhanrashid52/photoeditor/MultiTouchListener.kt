package ja.burhanrashid52.photoeditor

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

/**
 * Created on 18/01/2017.
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 *
 *
 */
class MultiTouchListener(
    private val mDeleteView: View?,
    photoEditorView: PhotoEditorView,
    photoEditImageView: ImageView?,
    private val mIsPinchScalable: Boolean,
//    onPhotoEditorListener: OnPhotoEditorListener?,
    viewState: PhotoEditorViewState,
    private val mOnTransformAction: OnTransformAction
) : OnTouchListener {
    private val mGestureListener: GestureDetector
    private val isRotateEnabled = true
    private val isTranslateEnabled = true
    private val isScaleEnabled = true
    private val minimumScale = 0.5f
    private val maximumScale = 10.0f
    private var mActivePointerId = INVALID_POINTER_ID
    private var mPrevX = 0f
    private var mPrevY = 0f
    private var mPrevRawX = 0f
    private var mPrevRawY = 0f
    private val mScaleGestureDetector: ScaleGestureDetector
    private val location = IntArray(2)
    private var outRect: Rect? = null
    private val deleteView: View?
    private val photoEditImageView: ImageView?
    private val photoEditorView: PhotoEditorView
    private var mOnGestureControl: OnGestureControl? = null
    internal var mOnPhotoEditorListener: OnPhotoEditorListener? = null
    private val viewState: PhotoEditorViewState
    private var initialTransform: ViewTransform? = null
    private lateinit var currentView: View

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        mScaleGestureDetector.onTouchEvent(view, event)
        mGestureListener.onTouchEvent(event)
        if (!isTranslateEnabled) {
            return true
        }
        val action = event.action
        val x = event.rawX.toInt()
        val y = event.rawY.toInt()
        currentView = view
        when (action and event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mPrevX = event.x
                mPrevY = event.y
                mPrevRawX = event.rawX
                mPrevRawY = event.rawY
                mActivePointerId = event.getPointerId(0)

                if (viewState.currentSelectedView !== view) {
                    viewState.currentSelectedView?.let {
                        val frmBorder = it.findViewById<FrameLayout>(R.id.frmBorder)
                        frmBorder?.setBackgroundResource(0)
                        val handles = mutableMapOf<Int, AdvancedTransformListener.HandleType>()
                        handles[R.id.handle_rotate] = AdvancedTransformListener.HandleType.ROTATE
                        handles[R.id.handle_top_left] = AdvancedTransformListener.HandleType.TOP_LEFT
                        handles[R.id.handle_top_right] = AdvancedTransformListener.HandleType.TOP_RIGHT
                        handles[R.id.handle_bottom_left] = AdvancedTransformListener.HandleType.BOTTOM_LEFT
                        handles[R.id.handle_bottom_right] = AdvancedTransformListener.HandleType.BOTTOM_RIGHT
                        for (id in handles.keys) {
                            it.findViewById<View>(id)?.visibility = View.INVISIBLE
                        }
                    }

                    viewState.currentSelectedView = view

                    val frmBorder = view.findViewById<FrameLayout>(R.id.frmBorder)
                    frmBorder?.setBackgroundResource(R.drawable.rounded_border_tv)
                    val handles = mutableMapOf<Int, AdvancedTransformListener.HandleType>()
                    handles[R.id.handle_rotate] = AdvancedTransformListener.HandleType.ROTATE
                    handles[R.id.handle_top_left] = AdvancedTransformListener.HandleType.TOP_LEFT
                    handles[R.id.handle_top_right] = AdvancedTransformListener.HandleType.TOP_RIGHT
                    handles[R.id.handle_bottom_left] = AdvancedTransformListener.HandleType.BOTTOM_LEFT
                    handles[R.id.handle_bottom_right] = AdvancedTransformListener.HandleType.BOTTOM_RIGHT
                    for (id in handles.keys) {
//                        view.findViewById<View>(id)?.visibility = View.VISIBLE
                        view.findViewById<View>(id)?.visibility = View.INVISIBLE
                    }

                }


                if (mDeleteView != null) {
                    mDeleteView.isEnabled = true
                }
                view.bringToFront()

                initialTransform = ViewTransform.from(view)

                firePhotoEditorSDKListener(view, true)
            }
            MotionEvent.ACTION_MOVE ->
                // Only enable dragging on focused stickers.
                if (view === viewState.currentSelectedView) {
                    val pointerIndexMove = event.findPointerIndex(mActivePointerId)
                    if (pointerIndexMove != -1) {
                        val currX = event.getX(pointerIndexMove)
                        val currY = event.getY(pointerIndexMove)
                        if (!mScaleGestureDetector.isInProgress) {
                            adjustTranslation(view, currX - mPrevX, currY - mPrevY)
                        }
                    }
                }
            MotionEvent.ACTION_CANCEL -> mActivePointerId = INVALID_POINTER_ID
            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
                if (!isViewInBounds(photoEditImageView, x, y)) {
                    view.animate().translationY(0f).translationY(0f)
                }
                if (mDeleteView != null) {
                    mDeleteView.isEnabled = false
                }
                val finalTransform = ViewTransform.from(view)
                if (initialTransform != null && initialTransform != finalTransform) {
                    mOnTransformAction.onTransform(view, initialTransform!!, finalTransform)
                }

                firePhotoEditorSDKListener(view, false)
                initialTransform = null
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndexPointerUp =
                    action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndexPointerUp)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndexPointerUp == 0) 1 else 0
                    mPrevX = event.getX(newPointerIndex)
                    mPrevY = event.getY(newPointerIndex)
                    mActivePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }

    fun getInitialTransform(): ViewTransform? {
        return initialTransform
    }

    private fun firePhotoEditorSDKListener(view: View, isStart: Boolean) {
        val tagData = view.tag as? Pair<*, *>
        val viewType = tagData?.first as? ViewType

        if (mOnPhotoEditorListener != null && viewType != null) {
            if (isStart) mOnPhotoEditorListener!!.onStartViewChangeListener(viewType)
            else mOnPhotoEditorListener!!.onStopViewChangeListener(viewType)
        }
    }

    private fun isViewInBounds(view: View?, x: Int, y: Int): Boolean {
        return view?.run {
            getDrawingRect(outRect)
            getLocationOnScreen(location)
            outRect?.offset(location[0], location[1])
            outRect?.contains(x, y)
        } ?: false
    }

    private inner class ScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var mPivotX = 0f
        private var mPivotY = 0f
        private val mPrevSpanVector = Vector2D()

        override fun onScaleBegin(view: View, detector: ScaleGestureDetector): Boolean {
            mPivotX = detector.getFocusX()
            mPivotY = detector.getFocusY()
            mPrevSpanVector.set(detector.getCurrentSpanVector())
            return mIsPinchScalable
        }

        override fun onScale(view: View, detector: ScaleGestureDetector): Boolean {
            val info = TransformInfo()
            info.deltaScale = if (isScaleEnabled) detector.getScaleFactor() else 1.0f
            info.deltaAngle = if (isRotateEnabled) Vector2D.getAngle(
                mPrevSpanVector,
                detector.getCurrentSpanVector()
            ) else 0.0f
            info.deltaX = if (isTranslateEnabled) detector.getFocusX() - mPivotX else 0.0f
            info.deltaY = if (isTranslateEnabled) detector.getFocusY() - mPivotY else 0.0f
            info.pivotX = mPivotX
            info.pivotY = mPivotY
            info.minimumScale = minimumScale
            info.maximumScale = maximumScale
            move(view, info)
            return !mIsPinchScalable
        }
    }

    private inner class TransformInfo {
        var deltaX = 0f
        var deltaY = 0f
        var deltaScale = 0f
        var deltaAngle = 0f
        var pivotX = 0f
        var pivotY = 0f
        var minimumScale = 0f
        var maximumScale = 0f
    }

    interface OnGestureControl {
        fun onClick(view: View)
        fun onLongClick(view: View)
    }

    fun setOnGestureControl(onGestureControl: OnGestureControl?) {
        mOnGestureControl = onGestureControl
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            mOnGestureControl?.onClick(currentView)

            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            mOnGestureControl?.onLongClick(currentView)
        }
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
        private fun adjustAngle(degrees: Float): Float {
            return when {
                degrees > 180.0f -> {
                    degrees - 360.0f
                }
                degrees < -180.0f -> {
                    degrees + 360.0f
                }
                else -> degrees
            }
        }

        private fun move(view: View, info: TransformInfo) {
            computeRenderOffset(view, info.pivotX, info.pivotY)
            adjustTranslation(view, info.deltaX, info.deltaY)
            var scale = view.scaleX * info.deltaScale
            scale = max(info.minimumScale, min(info.maximumScale, scale))
            view.scaleX = scale
            view.scaleY = scale
            val rotation = adjustAngle(view.rotation + info.deltaAngle)
            view.rotation = rotation
        }

        private fun adjustTranslation(view: View, deltaX: Float, deltaY: Float) {
            val deltaVector = floatArrayOf(deltaX, deltaY)
            view.matrix.mapVectors(deltaVector)
            view.translationX = view.translationX + deltaVector[0]
            view.translationY = view.translationY + deltaVector[1]
        }

        private fun computeRenderOffset(view: View, pivotX: Float, pivotY: Float) {
            if (view.pivotX == pivotX && view.pivotY == pivotY) {
                return
            }
            val prevPoint = floatArrayOf(0.0f, 0.0f)
            view.matrix.mapPoints(prevPoint)
            view.pivotX = pivotX
            view.pivotY = pivotY
            val currPoint = floatArrayOf(0.0f, 0.0f)
            view.matrix.mapPoints(currPoint)
            val offsetX = currPoint[0] - prevPoint[0]
            val offsetY = currPoint[1] - prevPoint[1]
            view.translationX = view.translationX - offsetX
            view.translationY = view.translationY - offsetY
        }
    }

    init {
        mScaleGestureDetector = ScaleGestureDetector(ScaleGestureListener())
        mGestureListener = GestureDetector(GestureListener())
        this.deleteView = mDeleteView
        this.photoEditorView = photoEditorView
        this.photoEditImageView = photoEditImageView
//        mOnPhotoEditorListener = onPhotoEditorListener
        outRect = if (deleteView != null) {
            Rect(
                deleteView.left, deleteView.top,
                deleteView.right, deleteView.bottom
            )
        } else {
            Rect(0, 0, 0, 0)
        }
        this.viewState = viewState
    }

    interface OnTransformAction {
        fun onTransform(view: View, oldTransform: ViewTransform, newTransform: ViewTransform)
    }
}