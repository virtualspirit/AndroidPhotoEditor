package ja.burhanrashid52.photoeditor

import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout

/**
 * Created by Burhanuddin Rashid on 15/05/21.
 *
 * @author <https:></https:>//github.com/burhanrashid52>
 */
class GraphicManager(
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

    fun undo(): Boolean {
        if (mViewState.undoActionsCount == 0) return false

        BoxHelper(mPhotoEditorView, mViewState).clearHelperBox()

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
                mViewState.currentSelectedView = view // Tetap set currentSelectedView

                // Panggil listener data, BUKAN listener UI
                (view.tag as? ViewType)?.let {
                    onPhotoEditorListener?.onAddViewListener(it, mViewState.addedViewsCount)
                }
            }
            ActionType.TRANSFORM -> {
                lastAction.oldTransform?.applyTo(lastAction.view)
            }
        }

        mViewState.pushRedoAction(lastAction)
        return mViewState.undoActionsCount > 0
    }

    fun redo(): Boolean {
        if (mViewState.redoActionsCount == 0) return false

        BoxHelper(mPhotoEditorView, mViewState).clearHelperBox()

        val lastRedoAction = mViewState.popRedoAction()

        when (lastRedoAction.actionType) {
            ActionType.ADD -> {
                val view = lastRedoAction.view
                mPhotoEditorView.addView(view)
                mViewState.addAddedView(view)

                mViewState.currentSelectedView = view
                (view.tag as? ViewType)?.let {
                    onPhotoEditorListener?.onAddViewListener(it, mViewState.addedViewsCount)
                }
            }
            ActionType.DELETE -> {
                val view = lastRedoAction.view
                mPhotoEditorView.removeView(view)
                mViewState.removeAddedView(view)

                mViewState.currentSelectedView = null

                (view.tag as? ViewType)?.let {
                    onPhotoEditorListener?.onRemoveViewListener(it, mViewState.addedViewsCount)
                }
            }
            ActionType.TRANSFORM -> {
                // Untuk me-redo TRANSFORM, terapkan state BARU
                lastRedoAction.newTransform?.applyTo(lastRedoAction.view)
            }
        }

        mViewState.pushUndoAction(lastRedoAction)
        return mViewState.redoActionsCount > 0
    }

    fun pushTransformAction(view: View, oldTransform: ViewTransform, newTransform: ViewTransform) {
        val action = EditorAction(view, ActionType.TRANSFORM, oldTransform, newTransform)
        mViewState.pushUndoAction(action)
        if (mViewState.redoActionsCount > 0) {
            mViewState.clearRedoActions()
        }
    }
}