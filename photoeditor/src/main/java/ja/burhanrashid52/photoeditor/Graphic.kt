package ja.burhanrashid52.photoeditor

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import ja.burhanrashid52.photoeditor.MultiTouchListener.OnGestureControl
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Created by Burhanuddin Rashid on 14/05/21.
 *
 * @author <https:></https:>//github.com/burhanrashid52>
 */
abstract class Graphic(
    val context: Context,
    val layoutId: Int,
    val viewType: ViewType,
    val graphicManager: GraphicManager?) {

    val rootView: View

    private var frmBorder: View? = null
    private var imgResize: ImageView? = null

    open fun updateView(view: View) {
        //Optional for subclass to override
    }

    init {
        if (layoutId == 0) {
            throw UnsupportedOperationException("Layout id cannot be zero. Please define a layout")
        }
        rootView = LayoutInflater.from(context).inflate(layoutId, null)
        setupView(rootView)
        setupRemoveView(rootView)
    }


    private fun setupRemoveView(rootView: View) {
        rootView.tag = Pair(viewType, this)
    }

    internal fun toggleSelection(selected: Boolean) {
        val frmBorder = rootView.findViewById<View>(R.id.frmBorder)
        frmBorder?.setBackgroundResource(R.drawable.rounded_border_tv)
        val imgResize = rootView.findViewById<ImageView>(R.id.imgPhotoEditorResize)
        imgResize?.visibility = View.VISIBLE

    }

    protected fun buildGestureController(
        photoEditorView: PhotoEditorView,
        viewState: PhotoEditorViewState
    ): OnGestureControl {
        val boxHelper = BoxHelper(photoEditorView, viewState)
        return object : OnGestureControl {
            override fun onClick(view: View) {
                boxHelper.clearHelperBox()
                toggleSelection(true)
                // Change the in-focus view
                viewState.currentSelectedView = rootView
                updateView(view)

            }

            override fun onLongClick(view: View) {
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    open fun setupView(rootView: View) {
        imgResize = rootView.findViewById(R.id.imgPhotoEditorResize)

        imgResize?.setOnTouchListener(ResizeRotateTouchListener(rootView))
    }

    class ResizeRotateTouchListener(private val viewToTransform: View) : View.OnTouchListener {
        // Properti untuk menghitung transformasi
        private var lastX = 0f
        private var lastY = 0f
        private var pivotX = 0f
        private var pivotY = 0f

        // Vektor untuk rotasi
        private val vector = Vector2D()

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Simpan posisi awal sentuhan dan pivot view
                    lastX = event.rawX
                    lastY = event.rawY

                    // Hitung pivot di tengah view
                    val location = IntArray(2)
                    viewToTransform.getLocationOnScreen(location)
                    pivotX = location[0] + viewToTransform.width / 2f
                    pivotY = location[1] + viewToTransform.height / 2f

                    // Beri tahu view parent agar tidak mengintersep touch event
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val currentX = event.rawX
                    val currentY = event.rawY

                    // Hitung rotasi
                    vector.set(lastX - pivotX, lastY - pivotY)
                    val lastAngle = Math.toDegrees(atan2(vector.y.toDouble(), vector.x.toDouble())).toFloat()

                    vector.set(currentX - pivotX, currentY - pivotY)
                    val currentAngle = Math.toDegrees(atan2(vector.y.toDouble(), vector.x.toDouble())).toFloat()

                    val rotation = currentAngle - lastAngle
                    viewToTransform.rotation += rotation

                    // Hitung skala
                    val lastDist = getDistance(pivotX, pivotY, lastX, lastY)
                    val currentDist = getDistance(pivotX, pivotY, currentX, currentY)
                    val scale = currentDist / lastDist

                    viewToTransform.scaleX *= scale
                    viewToTransform.scaleY *= scale

                    // Update posisi terakhir
                    lastX = currentX
                    lastY = currentY
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            return true
        }

        private fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            return sqrt(dx * dx + dy * dy)
        }
    }

}