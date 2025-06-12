package ja.burhanrashid52.photoeditor

import android.view.View
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeView

/**
 * Created for encapsulating Shape as a Graphic.
 * This class wraps a ShapeView and applies transformations via MultiTouchListener.
 */
internal class Shape(
    private val mPhotoEditorView: PhotoEditorView,
    private val mMultiTouchListener: MultiTouchListener,
    private val mViewState: PhotoEditorViewState,
    private val mGraphicManager: GraphicManager
) : Graphic(
    context = mPhotoEditorView.context,
    graphicManager = mGraphicManager,
    viewType = ViewType.BRUSH_DRAWING, // Or create a new ViewType.SHAPE
    layoutId = R.layout.view_photo_editor_shape
) {
    private var shapeView: ShapeView? = null

    fun buildView(shapeBuilder: ShapeBuilder, path: android.graphics.Path) {
        shapeView?.setShape(shapeBuilder, path)
    }


    private fun setupGesture() {
        val onGestureControl = buildGestureController(mPhotoEditorView, mViewState)
        mMultiTouchListener.setOnGestureControl(onGestureControl)
        rootView.setOnTouchListener(mMultiTouchListener)
    }

    override fun setupView(rootView: View) {
        shapeView = rootView.findViewById(R.id.shape_view)
    }

//    init {
//        setupGesture()
//    }
}