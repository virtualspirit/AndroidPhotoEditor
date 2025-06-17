package ja.burhanrashid52.photoeditor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import ja.burhanrashid52.photoeditor.MultiTouchListener.OnGestureControl

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

    internal fun toggleSelection() {
        val frmBorder = rootView.findViewById<View>(R.id.frmBorder)
        frmBorder?.setBackgroundResource(R.drawable.rounded_border_tv)
    }

    protected fun buildGestureController(
        photoEditorView: PhotoEditorView,
        viewState: PhotoEditorViewState
    ): OnGestureControl {
        val boxHelper = BoxHelper(photoEditorView, viewState)
        return object : OnGestureControl {
            override fun onClick(view: View) {
                boxHelper.clearHelperBox()
                toggleSelection()
                // Change the in-focus view
                viewState.currentSelectedView = rootView
                updateView(view)

            }

            override fun onLongClick(view: View) {
            }
        }
    }

    open fun setupView(rootView: View) {}
}