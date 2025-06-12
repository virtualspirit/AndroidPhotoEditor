package ja.burhanrashid52.photoeditor

import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout

/**
 * Created by Burhanuddin Rashid on 15/05/21.
 *
 * @author <https:></https:>//github.com/burhanrashid52>
 */
internal class GraphicManager(
    private val mPhotoEditorView: PhotoEditorView,
    private val mViewState: PhotoEditorViewState
) {

    var onPhotoEditorListener: OnPhotoEditorListener? = null

//    val redoStackCount
//        get() = mViewState.redoViewsCount

    fun addView(graphic: Graphic) {
        val view = graphic.rootView

        var params = view.layoutParams

        if (params == null) {
            params = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            (params as RelativeLayout.LayoutParams).addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        }

        mPhotoEditorView.addView(view, params) // Gunakan params yang sudah diperiksa
        mViewState.addAddedView(view)

//        if (redoStackCount > 0) {
//            mViewState.clearRedoViews()
//        }

        // Simpan aksi "ADD" ke tumpukan undo
        mViewState.pushUndoAction(EditorAction(view, ActionType.ADD))

        // Setiap ada aksi baru, tumpukan redo harus bersih
        if (mViewState.redoActionsCount > 0) {
            mViewState.clearRedoActions()
        }

        onPhotoEditorListener?.onAddViewListener(
            graphic.viewType,
            mViewState.addedViewsCount
        )
    }

//    fun removeView(graphic: Graphic) {
//        val view = graphic.rootView
//        if (mViewState.containsAddedView(view)) {
//            mPhotoEditorView.removeView(view)
//            mViewState.removeAddedView(view)
//            mViewState.pushRedoView(view)
//            onPhotoEditorListener?.onRemoveViewListener(
//                graphic.viewType,
//                mViewState.addedViewsCount
//            )
//        }
//    }

    fun removeView(viewToRemove: View): Boolean {
        if (mViewState.containsAddedView(viewToRemove)) {
            mPhotoEditorView.removeView(viewToRemove)
            mViewState.removeAddedView(viewToRemove)

            mViewState.pushUndoAction(EditorAction(viewToRemove, ActionType.DELETE))

            if (mViewState.redoActionsCount > 0) {
                mViewState.clearRedoActions()
            }

            (viewToRemove.tag as? ViewType)?.let {
                onPhotoEditorListener?.onRemoveViewListener(it, mViewState.addedViewsCount)
            }
            return true
        }
        return false
    }

    fun updateView(view: View) {
        mPhotoEditorView.updateViewLayout(view, view.layoutParams)
        mViewState.replaceAddedView(view)
    }

//    fun undoView(): Boolean {
//        if (mViewState.addedViewsCount > 0) {
//            val removeView = mViewState.getAddedView(
//                mViewState.addedViewsCount - 1
//            )
//            if (removeView is DrawingView) {
//                return removeView.undo() || (mViewState.addedViewsCount != 0)
//            } else {
//                mViewState.removeAddedView(mViewState.addedViewsCount - 1)
//                mPhotoEditorView.removeView(removeView)
//                mViewState.pushRedoView(removeView)
//            }
//            when (val viewTag = removeView.tag) {
//                is ViewType -> onPhotoEditorListener?.onRemoveViewListener(
//                    viewTag,
//                    mViewState.addedViewsCount
//                )
//            }
//        }
//        return mViewState.addedViewsCount != 0
//    }
//
//    fun redoView(): Boolean {
//        if (redoStackCount > 0) {
//            val redoView = mViewState.getRedoView(redoStackCount - 1)
//
//            if (redoView is DrawingView) {
//                val result = redoView.redo()
//                return result || redoStackCount > 0
//            } else {
//                mViewState.popRedoView()
//                mPhotoEditorView.addView(redoView)
//                mViewState.addAddedView(redoView)
//            }
//
//            val viewTag = redoView.tag
//            if (viewTag is ViewType) {
//                onPhotoEditorListener?.onAddViewListener(viewTag, mViewState.addedViewsCount)
//            }
//        }
//
//        return redoStackCount > 0
//    }

    fun undo(): Boolean {
        if (mViewState.undoActionsCount == 0) return false

        val lastAction = mViewState.popUndoAction()

        when (lastAction.actionType) {
            ActionType.ADD -> {
                val view = lastAction.view
                mPhotoEditorView.removeView(view)
                mViewState.removeAddedView(view)

                (view.tag as? ViewType)?.let {
                    onPhotoEditorListener?.onRemoveViewListener(it, mViewState.addedViewsCount)
                }
            }
            ActionType.DELETE -> {
                val view = lastAction.view
                mPhotoEditorView.addView(view)
                mViewState.addAddedView(view)

                (view.tag as? ViewType)?.let {
                    onPhotoEditorListener?.onAddViewListener(it, mViewState.addedViewsCount)
                }
            }
        }

        mViewState.pushRedoAction(lastAction)
        return mViewState.undoActionsCount > 0
    }

    fun redo(): Boolean {
        if (mViewState.redoActionsCount == 0) return false

        val lastRedoAction = mViewState.popRedoAction()

        when (lastRedoAction.actionType) {
            ActionType.ADD -> {
                val view = lastRedoAction.view
                mPhotoEditorView.addView(view)
                mViewState.addAddedView(view)
            }
            ActionType.DELETE -> {
                val view = lastRedoAction.view
                mPhotoEditorView.removeView(view)
                mViewState.removeAddedView(view)
            }
        }

        mViewState.pushUndoAction(lastRedoAction)
        return mViewState.redoActionsCount > 0
    }

//    fun removeViewBy(viewToRemove: View): Boolean {
//        if (mViewState.containsAddedView(viewToRemove)) {
//            mPhotoEditorView.removeView(viewToRemove)
//            mViewState.removeAddedView(viewToRemove)
//            mViewState.pushRedoView(viewToRemove)
//            (viewToRemove.tag as? ViewType)?.let {
//                onPhotoEditorListener?.onRemoveViewListener(it, mViewState.addedViewsCount)
//            }
//            return true
//        }
//        return false
//    }
}