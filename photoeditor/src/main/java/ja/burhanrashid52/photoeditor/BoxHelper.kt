package ja.burhanrashid52.photoeditor

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * Created by Burhanuddin Rashid on 18/05/21.
 *
 * @author <https:></https:>//github.com/burhanrashid52>
 */
internal class BoxHelper(
    private val mPhotoEditorView: PhotoEditorView,
    private val mViewState: PhotoEditorViewState
) {
    fun clearHelperBox() {
        for (i in 0 until mPhotoEditorView.childCount) {
            val childAt = mPhotoEditorView.getChildAt(i)
            val frmBorder = childAt.findViewById<FrameLayout>(R.id.frmBorder)
            frmBorder?.setBackgroundResource(0)
//            val imgResize = childAt.findViewById<ImageView>(R.id.imgPhotoEditorResize)
//            imgResize?.visibility = View.GONE
            val handles = mutableMapOf<Int, AdvancedTransformListener.HandleType>()
            handles[R.id.handle_rotate] = AdvancedTransformListener.HandleType.ROTATE
            handles[R.id.handle_top_left] = AdvancedTransformListener.HandleType.TOP_LEFT
            handles[R.id.handle_top_right] = AdvancedTransformListener.HandleType.TOP_RIGHT
            handles[R.id.handle_bottom_left] = AdvancedTransformListener.HandleType.BOTTOM_LEFT
            handles[R.id.handle_bottom_right] = AdvancedTransformListener.HandleType.BOTTOM_RIGHT
            for (id in handles.keys) {
                childAt.findViewById<View>(id)?.visibility = View.INVISIBLE
            }
        }
        mViewState.clearCurrentSelectedView()
        mViewState.deleteView?.isEnabled = false
    }

    fun clearAllViews(drawingView: DrawingView?) {
        for (i in 0 until mViewState.addedViewsCount) {
            mPhotoEditorView.removeView(mViewState.getAddedView(i))
        }
        drawingView?.let {
            if (mViewState.containsAddedView(it)) {
                mPhotoEditorView.addView(it)
            }
        }

        mViewState.clearAddedViews()
        mViewState.clearRedoActions()
        mViewState.clearUndoActions()
        drawingView?.clearAll()
    }
}