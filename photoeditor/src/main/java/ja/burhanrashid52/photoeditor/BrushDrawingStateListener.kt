package ja.burhanrashid52.photoeditor

import ja.burhanrashid52.photoeditor.shape.AbstractShape

/**
 * Created by Burhanuddin Rashid on 17/05/21.
 *
 * @author <https:></https:>//github.com/burhanrashid52>
 */
class BrushDrawingStateListener internal constructor(
    private val mPhotoEditorView: PhotoEditorView,
    private val mViewState: PhotoEditorViewState
) : BrushViewChangeListener {
    private var mOnPhotoEditorListener: OnPhotoEditorListener? = null
    fun setOnPhotoEditorListener(onPhotoEditorListener: OnPhotoEditorListener?) {
        mOnPhotoEditorListener = onPhotoEditorListener
    }

    override fun onViewAdd(drawingView: DrawingView) {
        if (mViewState.redoActionsCount > 0) {
            mViewState.popRedoAction()
        }
        mViewState.addAddedView(drawingView)
        mOnPhotoEditorListener?.onAddViewListener(
            ViewType.BRUSH_DRAWING,
            mViewState.addedViewsCount
        )
    }

    override fun onViewRemoved(drawingView: DrawingView) {
        if (mViewState.addedViewsCount > 0) {
            val removeView = mViewState.removeAddedView(
                mViewState.addedViewsCount - 1
            )
            if (removeView !is DrawingView) {
                mPhotoEditorView.removeView(removeView)
            }
            val lastAction = mViewState.popUndoAction()
            mViewState.pushRedoAction(lastAction)
        }
        mOnPhotoEditorListener?.onRemoveViewListener(
            ViewType.BRUSH_DRAWING,
            mViewState.addedViewsCount
        )
    }

    override fun onStartDrawing() {
        mOnPhotoEditorListener?.onStartViewChangeListener(ViewType.BRUSH_DRAWING)

    }

    override fun onStopDrawing() {
        if (mViewState.redoActionsCount > 0) {
            mViewState.clearRedoActions()
        }
        mOnPhotoEditorListener?.onStopViewChangeListener(ViewType.BRUSH_DRAWING)
    }

    override fun onShapeCreated(
        shape: AbstractShape,
        touchX: Float,
        touchY: Float
    ) {
        TODO("Not yet implemented")
    }
}