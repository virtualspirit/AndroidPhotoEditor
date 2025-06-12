package ja.burhanrashid52.photoeditor

import android.graphics.Path
import android.view.View
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeView

/**
 * Created for encapsulating Shape as a Graphic.
 * This class wraps a ShapeView and applies transformations via MultiTouchListener.
 */

data class ShapeRecipe(val shapeBuilder: ShapeBuilder, val path: Path)

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
    private var recipe: ShapeRecipe? = null

    fun buildView(shapeBuilder: ShapeBuilder, path: Path) {
        this.recipe = ShapeRecipe(shapeBuilder, path)
        shapeView?.setShape(shapeBuilder, path)
    }

    fun getRecipe(): ShapeRecipe? {
        return recipe
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