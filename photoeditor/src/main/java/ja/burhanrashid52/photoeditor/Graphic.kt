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

    private val handles = mutableMapOf<Int, AdvancedTransformListener.HandleType>()
    private val allHandles = mutableListOf<View>()

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

    fun updateHandlesScale() {
        val inverseScaleX = if (rootView.scaleX == 0f) 1f else 1f / rootView.scaleX
        val inverseScaleY = if (rootView.scaleY == 0f) 1f else 1f / rootView.scaleY
        allHandles.forEach { handle ->
            handle.scaleX = inverseScaleX
            handle.scaleY = inverseScaleY
        }
    }

    internal fun toggleSelection(selected: Boolean) {
        val frmBorder = rootView.findViewById<View>(R.id.frmBorder)
        frmBorder?.setBackgroundResource(R.drawable.rounded_border_tv)
//        val visibility = View.INVISIBLE
        val visibility = View.VISIBLE

        allHandles.forEach { it.visibility = visibility }
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
        val handleIds = listOf(
            R.id.handle_rotate, R.id.handle_top_left, R.id.handle_top_right,
            R.id.handle_bottom_left, R.id.handle_bottom_right
        )

        val handleTypes = mapOf(
            R.id.handle_rotate to AdvancedTransformListener.HandleType.ROTATE,
            R.id.handle_top_left to AdvancedTransformListener.HandleType.TOP_LEFT,
            R.id.handle_top_right to AdvancedTransformListener.HandleType.TOP_RIGHT,
            R.id.handle_bottom_left to AdvancedTransformListener.HandleType.BOTTOM_LEFT,
            R.id.handle_bottom_right to AdvancedTransformListener.HandleType.BOTTOM_RIGHT,
        )

        handleIds.forEach { id ->
            val handleView = rootView.findViewById<View>(id)
            if (handleView != null) {
                allHandles.add(handleView)
                handleTypes[id]?.let { type ->
                    handleView.setOnTouchListener(AdvancedTransformListener(this.rootView, type))
//                    handleView.setOnTouchListener(AdvancedTransformListener(this.rootView, type, allHandles))
                }
            }
        }

    }

}