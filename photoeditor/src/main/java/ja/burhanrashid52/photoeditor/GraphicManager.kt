package ja.burhanrashid52.photoeditor

import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import ja.burhanrashid52.photoediting.StrokeStyle
import ja.burhanrashid52.photoeditor.shape.ShapeView

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

        mPhotoEditorView.addView(view, params)
        mViewState.addAddedView(view)

//        if (redoStackCount > 0) {
//            mViewState.clearRedoViews()
//        }

        mViewState.pushUndoAction(EditorAction(view, ActionType.ADD))

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
                mViewState.currentSelectedView = view

                (view.tag as? ViewType)?.let {
                    onPhotoEditorListener?.onAddViewListener(it, mViewState.addedViewsCount)
                }
            }
            ActionType.TRANSFORM -> {
                lastAction.oldTransform?.applyTo(lastAction.view)
            }
            ActionType.CHANGE_COLOR -> {
                val view = lastAction.view
                val colorToApply = lastAction.oldColor
                if (colorToApply != null) {
                    applyColorToView(view, colorToApply)
                }
            }
            ActionType.CHANGE_STROKE -> {
                val view = lastAction.view
                val widthToApply = lastAction.oldStrokeWidth
                if (widthToApply != null) {
                    applyStrokeWidthToView(view, widthToApply)
                }
            }
            ActionType.CHANGE_STROKE_STYLE -> {
                applyStrokeStyleToView(lastAction.view, lastAction.oldStrokeStyle as StrokeStyle)
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
                lastRedoAction.newTransform?.applyTo(lastRedoAction.view)
            }

            ActionType.CHANGE_COLOR -> {
                val view = lastRedoAction.view
                val colorToApply = lastRedoAction.newColor
                if (colorToApply != null) {
                    applyColorToView(view, colorToApply)
                }
            }
            ActionType.CHANGE_STROKE -> {
                val view = lastRedoAction.view
                val widthToApply = lastRedoAction.newStrokeWidth
                if (widthToApply != null) {
                    applyStrokeWidthToView(view, widthToApply)
                }
            }
            ActionType.CHANGE_STROKE_STYLE -> {
                applyStrokeStyleToView(lastRedoAction.view, lastRedoAction.newStrokeStyle as StrokeStyle)
            }
        }

        mViewState.pushUndoAction(lastRedoAction)
        return mViewState.redoActionsCount > 0
    }

    private fun applyColorToView(view: View, color: Int) {
        val viewType = if (view.tag is ViewType) {
            view.tag as ViewType
        } else if (view.tag is Pair<*, *>) {
            (view.tag as Pair<*, *>).first as? ViewType
        } else {
            null
        }

        when (viewType) {
            ViewType.TEXT -> {
                view.findViewById<TextView>(R.id.tvPhotoEditorText)?.setTextColor(color)
            }
            ViewType.BRUSH_DRAWING -> {
                view.findViewById<ShapeView>(R.id.shape_view)?.updateColor(color)
            }
            else -> {}
        }
    }

    fun pushTransformAction(view: View, oldTransform: ViewTransform, newTransform: ViewTransform) {
        val action = EditorAction(view, ActionType.TRANSFORM, oldTransform, newTransform)
        mViewState.pushUndoAction(action)
        if (mViewState.redoActionsCount > 0) {
            mViewState.clearRedoActions()
        }
    }

    fun pushUndoAction(action: EditorAction) {
        mViewState.pushUndoAction(action)
        if (mViewState.redoActionsCount > 0) {
            mViewState.clearRedoActions()
        }
    }

    private fun applyStrokeWidthToView(view: View, width: Float) {
        view.findViewById<ShapeView>(R.id.shape_view)?.updateStrokeWidth(width)
    }

    private fun applyStrokeStyleToView(view: View, style: StrokeStyle) {
        view.findViewById<ShapeView>(R.id.shape_view)?.updateStrokeStyle(style)
    }
}