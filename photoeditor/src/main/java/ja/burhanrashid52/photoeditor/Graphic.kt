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
        //We are setting tag as ViewType to identify what type of the view it is
        //when we remove the view from stack i.e onRemoveViewListener(ViewType viewType, int numberOfAddedViews);
//        rootView.tag = viewType
//        rootView.tag = Pair(viewType, this)
//        val imgClose = rootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)
//        imgClose?.setOnClickListener {
////            graphicManager?.removeView(this@Graphic)
//        }

//        rootView.tag = viewType
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
            override fun onClick() {
                boxHelper.clearHelperBox()
                toggleSelection()
                // Change the in-focus view
                viewState.currentSelectedView = rootView
            }

            override fun onLongClick() {
                updateView(rootView)
            }
        }
    }

    open fun setupView(rootView: View) {}
}