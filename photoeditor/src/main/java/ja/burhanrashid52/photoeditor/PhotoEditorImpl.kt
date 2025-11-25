package ja.burhanrashid52.photoeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.annotation.RequiresPermission
import androidx.constraintlayout.widget.ConstraintLayout
import ja.burhanrashid52.photoediting.EditImageActivity
import ja.burhanrashid52.photoediting.StrokeStyle
import ja.burhanrashid52.photoeditor.PhotoEditorImageViewListener.OnSingleTapUpCallback
import ja.burhanrashid52.photoeditor.TextStyleBuilder.TextStyle
import ja.burhanrashid52.photoeditor.shape.AbstractShape
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *
 *
 * This class in initialize by [PhotoEditor.Builder] using a builder pattern with multiple
 * editing attributes
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 18/01/2017
 */
internal class PhotoEditorImpl @SuppressLint("ClickableViewAccessibility") constructor(
    builder: PhotoEditor.Builder
) : PhotoEditor, BrushViewChangeListener, MultiTouchListener.OnTransformAction {
    private val photoEditorView: PhotoEditorView = builder.photoEditorView
    private val viewState: PhotoEditorViewState = PhotoEditorViewState()
    private val imageView: ImageView = builder.imageView
    private val deleteView: View? = builder.deleteView
    private val drawingView: DrawingView = builder.drawingView
    private val mBoxHelper: BoxHelper = BoxHelper(builder.photoEditorView, viewState)
    private var mOnPhotoEditorListener: OnPhotoEditorListener? = null
    private val isTextPinchScalable: Boolean = builder.isTextPinchScalable
    private val mDefaultTextTypeface: Typeface? = builder.textTypeface
    private val mDefaultEmojiTypeface: Typeface? = builder.emojiTypeface
    private val mGraphicManager: GraphicManager = GraphicManager(builder.photoEditorView, viewState)
    private val context: Context = builder.context
    private val mMultiTouchListener: MultiTouchListener
    private val viewToGraphicMap = mutableMapOf<View, Graphic>()

    private val resources: Resources = context.resources

    override fun addImage(desiredImage: Bitmap): Sticker  {
        val multiTouchListener = getMultiTouchListener(true)
        val sticker = Sticker(photoEditorView, multiTouchListener, viewState, mGraphicManager)
        sticker.buildView(desiredImage)
        addToEditor(sticker)

        return sticker
    }

    override fun addText(text: String, colorCodeTextView: Int) {
        addText(null, text, colorCodeTextView)
    }

    override fun addText(textTypeface: Typeface?, text: String, colorCodeTextView: Int) {
        val styleBuilder = TextStyleBuilder()
        styleBuilder.withTextColor(colorCodeTextView)
        if (textTypeface != null) {
            styleBuilder.withTextFont(textTypeface)
        }
        addText(text, styleBuilder)
    }

    override fun addText(text: String, styleBuilder: TextStyleBuilder?): Text {
        drawingView.enableDrawing(false)
        val textGraphic = Text(
            photoEditorView,
            mMultiTouchListener,
            viewState,
            mDefaultTextTypeface,
            mGraphicManager
        )
        textGraphic.buildView(text, styleBuilder)
        addToEditor(textGraphic)
        return textGraphic
    }

    override fun editText(view: View, inputText: String, colorCode: Int) {
        editText(view, null, inputText, colorCode)
    }

    override fun editText(view: View, textTypeface: Typeface?, inputText: String, colorCode: Int) {
        val styleBuilder = TextStyleBuilder()
        styleBuilder.withTextColor(colorCode)
        if (textTypeface != null) {
            styleBuilder.withTextFont(textTypeface)
        }
        editText(view, inputText, styleBuilder)
    }

    override fun editText(view: View, inputText: String, styleBuilder: TextStyleBuilder?) {
        val inputTextView = view.findViewById<TextView>(R.id.tvPhotoEditorText)
        if (inputTextView != null && viewState.containsAddedView(view) && !TextUtils.isEmpty(
                inputText
            )
        ) {

            val oldStateBuilder = TextStyleBuilder()
            oldStateBuilder.values[TextStyle.TEXT] = inputTextView.text
            val oldTextSizeInSp = inputTextView.textSize / context.resources.displayMetrics.scaledDensity
            oldStateBuilder.withTextSize(oldTextSizeInSp)
            oldStateBuilder.withTextColor(inputTextView.currentTextColor)
            (inputTextView.background as? ColorDrawable)?.let {
                oldStateBuilder.withBackgroundColor(it.color)
            }

            val newStateBuilder = styleBuilder ?: TextStyleBuilder()
            newStateBuilder.values[TextStyle.TEXT] = inputText

            val editAction = EditorAction(view, actionType = ActionType.CHANGE_TEXT, oldTextStyle = oldStateBuilder, newTextStyle = newStateBuilder)
            mGraphicManager.pushUndoAction(editAction)

            inputTextView.text = inputText
            styleBuilder?.applyStyle(inputTextView)
            mGraphicManager.updateView(view)
        }
    }

    override fun addEmoji(emojiName: String) {
        addEmoji(null, emojiName)
    }

    override fun addEmoji(emojiTypeface: Typeface?, emojiName: String) {
        drawingView.enableDrawing(false)
        val multiTouchListener = getMultiTouchListener(true)
        val emoji = Emoji(
            photoEditorView,
            multiTouchListener,
            viewState,
            mGraphicManager,
            mDefaultEmojiTypeface
        )
        emoji.buildView(emojiTypeface, emojiName)
        addToEditor(emoji)
    }

    private fun addToEditor(graphic: Graphic, clearFocus: Boolean = true) {
        if (clearFocus) {
            clearHelperBox()
        }
        graphic.rootView.setOnTouchListener(mMultiTouchListener)

        mGraphicManager.addView(graphic)

        viewToGraphicMap[graphic.rootView] = graphic

        graphic.updateHandlesScale()
        viewState.currentSelectedView = graphic.rootView
        graphic.toggleSelection(true)
        mOnPhotoEditorListener?.onStartViewChangeListener(graphic.viewType)
    }

    private fun addShape(shapeBuilder: ShapeBuilder, path: Path): Shape {
        val shapeGraphic = Shape(photoEditorView,mMultiTouchListener,  viewState, mGraphicManager)
        shapeGraphic.buildView(shapeBuilder, path)

        val bounds = RectF()
        path.computeBounds(bounds, true)

        val halfStrokeWidth = shapeBuilder.shapeSize / 2f
        val newWidth = Math.ceil((bounds.width() + shapeBuilder.shapeSize).toDouble()).toInt() + 2
        val newHeight = Math.ceil((bounds.height() + shapeBuilder.shapeSize).toDouble()).toInt() + 2

        val params = RelativeLayout.LayoutParams(newWidth, newHeight)
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        shapeGraphic.rootView.layoutParams = params

        addToEditor(shapeGraphic)

        return shapeGraphic
    }

    /**
     * Create a new instance and scalable touchview
     *
     * @param isPinchScalable true if make pinch-scalable, false otherwise.
     * @return scalable multitouch listener
     */
    private fun getMultiTouchListener(isPinchScalable: Boolean): MultiTouchListener {
        return mMultiTouchListener
    }

    override fun onStopViewChangeListener(viewType: ViewType) {
        val stoppedView = viewState.currentSelectedView
        if (stoppedView != null) {
            val initialTransform = mMultiTouchListener.getInitialTransform()
            val finalTransform = ViewTransform.from(stoppedView)

            if (initialTransform != null && initialTransform != finalTransform) {
                mGraphicManager.pushTransformAction(stoppedView, initialTransform, finalTransform)
            }
        }
        mOnPhotoEditorListener?.onStopViewChangeListener(viewType)
    }

    override fun duplicateSelectedView(): Boolean {
        val currentView = viewState.currentSelectedView ?: return false // Langsung return jika null

        val tagData = currentView.tag as? Pair<*, *> ?: return false
        val viewType = tagData.first as? ViewType ?: return false
        val originalGraphic = tagData.second as? Graphic ?: return false

        if (viewType == ViewType.BRUSH_DRAWING && originalGraphic is Shape) {
            val originalShapeRecipe = originalGraphic.getRecipe() ?: return false

            // Buat instance graphic baru untuk duplikat
            val duplicatedGraphic = Shape(photoEditorView, mMultiTouchListener, viewState, mGraphicManager)

            // Panggil buildView untuk mengatur path dan paint di dalam ShapeView
            duplicatedGraphic.buildView(originalShapeRecipe.shapeBuilder, originalShapeRecipe.path)

            // --- LOGIKA BARU UNTUK DUPLIKASI YANG TEPAT ---

            // 1. Salin LayoutParams dari view asli ke duplikat
            val originalParams = currentView.layoutParams as RelativeLayout.LayoutParams
            val duplicatedRootView = duplicatedGraphic.rootView
            duplicatedRootView.layoutParams = RelativeLayout.LayoutParams(originalParams) // Salin semua parameter

            // 2. Salin properti transformasi
            duplicatedRootView.rotation = currentView.rotation
            duplicatedRootView.scaleX = currentView.scaleX
            duplicatedRootView.scaleY = currentView.scaleY

            // 3. Beri sedikit offset agar tidak tumpang tindih
            val offset = 30f
            duplicatedRootView.translationX = currentView.translationX + offset
            duplicatedRootView.translationY = currentView.translationY + offset

            // 4. Salin ukuran frmBorder dari asli ke duplikat (PENTING!)
            val originalFrmBorder = currentView.findViewById<FrameLayout>(R.id.frmBorder)
            val duplicatedFrmBorder = duplicatedRootView.findViewById<FrameLayout>(R.id.frmBorder)
            val borderParams = duplicatedFrmBorder.layoutParams
            borderParams.width = originalFrmBorder.width
            borderParams.height = originalFrmBorder.height
            duplicatedFrmBorder.layoutParams = borderParams

            val shapeView = duplicatedRootView.findViewById<ShapeView>(R.id.shape_view)
            if (shapeView != null) {
                val avgScale = (duplicatedRootView.scaleX + duplicatedRootView.scaleY) / 2f
                shapeView.setParentScale(avgScale)
            }

            duplicatedGraphic.updateHandlesScale()

            addToEditor(duplicatedGraphic)
            return true
        } else if (viewType == ViewType.TEXT) {
            // ... (logika duplikasi teks tetap sama)
            return true
        } else if (viewType == ViewType.IMAGE) {
            val originalImageView = currentView.findViewById<ImageView>(R.id.imgPhotoEditorImage)
            val bitmap = (originalImageView?.drawable as? BitmapDrawable)?.bitmap ?: return false

            val duplicatedGraphic = Sticker(photoEditorView, mMultiTouchListener, viewState, mGraphicManager)
            duplicatedGraphic.buildView(bitmap)

            val duplicatedRootView = duplicatedGraphic.rootView

            val originalParams = currentView.layoutParams as RelativeLayout.LayoutParams
            duplicatedRootView.layoutParams = RelativeLayout.LayoutParams(originalParams)

            duplicatedRootView.rotation = currentView.rotation
            duplicatedRootView.scaleX = currentView.scaleX
            duplicatedRootView.scaleY = currentView.scaleY

            val offset = 30f
            duplicatedRootView.translationX = currentView.translationX + offset
            duplicatedRootView.translationY = currentView.translationY + offset

            duplicatedGraphic.updateHandlesScale()

            addToEditor(duplicatedGraphic)
            return true
        }

        return false
    }

    override fun addCropAction(oldBitmap: Bitmap, newBitmap: Bitmap) {
        val action = EditorAction(
            view = photoEditorView,
            actionType = ActionType.CROP,
            oldBitmap = oldBitmap,
            newBitmap = newBitmap
        )
        mGraphicManager.pushUndoAction(action)
    }

    override fun addFilterAction(oldFilter: PhotoFilter, newFilter: PhotoFilter) {
        val filterAction = EditorAction(
            view = photoEditorView,
            actionType = ActionType.FILTER,
            oldFilter = oldFilter,
            newFilter= newFilter)
        mGraphicManager.pushUndoAction(filterAction)
    }

    override fun setBrushDrawingMode(brushDrawingMode: Boolean) {
        Log.d("DrawingView", "Entering Shape Creating Mode...")
        drawingView.enableDrawing(brushDrawingMode)
    }

    override val brushDrawableMode: Boolean
        get() = drawingView != null && drawingView.isDrawingEnabled

    override fun setOpacity(@IntRange(from = 0, to = 100) opacity: Int) {
        var opacityValue = opacity
        opacityValue = (opacityValue / 100.0 * 255.0).toInt()
        drawingView.currentShapeBuilder.withShapeOpacity(opacityValue)
    }

    override var brushSize: Float
        get() = drawingView.currentShapeBuilder.shapeSize
        set(size) {
            drawingView.currentShapeBuilder.withShapeSize(size)
        }
    override var brushColor: Int
        get() = drawingView.currentShapeBuilder.shapeColor
        set(color) {
            drawingView.currentShapeBuilder.withShapeColor(color)
        }

    override fun clearAllViews() {
        mBoxHelper.clearAllViews(drawingView)
    }

    override fun clearHelperBox() {
        mBoxHelper.clearHelperBox()
    }

    override fun setFilterEffect(customEffect: CustomEffect?) {
        photoEditorView.setFilterEffect(customEffect)
    }

    override fun setFilterEffect(filterType: PhotoFilter) {
        photoEditorView.setFilterEffect(filterType)
    }

    override fun setFilterEffect(sourceBitmap: Bitmap, filterType: PhotoFilter) {
        photoEditorView.setFilterEffect(sourceBitmap, filterType)
    }

    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override suspend fun saveAsFile(
        imagePath: String,
        saveSettings: SaveSettings
    ): SaveFileResult = withContext(Dispatchers.Main) {
        photoEditorView.saveFilter()
        val photoSaverTask = PhotoSaverTask(photoEditorView, mBoxHelper, saveSettings)
        return@withContext photoSaverTask.saveImageAsFile(imagePath)
    }

    override suspend fun saveAsBitmap(
        saveSettings: SaveSettings
    ): Bitmap = withContext(Dispatchers.Main) {
        photoEditorView.saveFilter()
        val photoSaverTask = PhotoSaverTask(photoEditorView, mBoxHelper, saveSettings)
        return@withContext photoSaverTask.saveImageAsBitmap()
    }

    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override fun saveAsFile(
        imagePath: String,
        saveSettings: SaveSettings,
        onSaveListener: PhotoEditor.OnSaveListener
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            when (val result = saveAsFile(imagePath, saveSettings)) {
                is SaveFileResult.Success -> onSaveListener.onSuccess(imagePath)
                is SaveFileResult.Failure -> onSaveListener.onFailure(result.exception)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    override fun saveAsFile(imagePath: String, onSaveListener: PhotoEditor.OnSaveListener) {
        saveAsFile(imagePath, SaveSettings.Builder().build(), onSaveListener)
    }

    override fun saveAsBitmap(saveSettings: SaveSettings, onSaveBitmap: OnSaveBitmap) {
        GlobalScope.launch(Dispatchers.Main) {
            val bitmap = saveAsBitmap(saveSettings)
            onSaveBitmap.onBitmapReady(bitmap)
        }
    }

    override fun saveAsBitmap(onSaveBitmap: OnSaveBitmap) {
        saveAsBitmap(SaveSettings.Builder().build(), onSaveBitmap)
    }

    override val isCacheEmpty: Boolean
        get() = !isUndoAvailable && !isRedoAvailable

    // region Shape
    override fun setShape(shapeBuilder: ShapeBuilder) {
        drawingView.currentShapeBuilder = shapeBuilder
    } // endregion

    override fun changeSelectedViewColor(newColor: Int) {
        Log.e("wew", "PhotoEditor.changeSelectedViewColor called with color: $newColor")
        val currentView = viewState.currentSelectedView
        if (currentView == null) {
            Log.e("wew", "Cannot change color, currentSelectedView is null!")
            return
        }
        Log.e("wew", "currentSelectedView is: ${currentView.javaClass.simpleName}, tag: ${currentView.tag}")

        val viewType = if (currentView.tag is ViewType) {
            currentView.tag as ViewType
        } else if (currentView.tag is Pair<*, *>) {
            (currentView.tag as Pair<*, *>).first as? ViewType
        } else {
            null
        }

        Log.e("wew", "Detected ViewType: $viewType")
        var oldColor: Int? = null

        when (viewType) {
            ViewType.TEXT -> {
                val textView = currentView.findViewById<TextView>(R.id.tvPhotoEditorText)
                oldColor = textView?.currentTextColor
                textView?.setTextColor(newColor)
            }
            ViewType.BRUSH_DRAWING -> {
                Log.e("wew", "Applying color to SHAPE view (BRUSH_DRAWING).")
                val shapeView = currentView.findViewById<ShapeView>(R.id.shape_view)
                oldColor = shapeView?.getCurrentColor()

                if (shapeView != null) {
                    shapeView.updateColor(newColor)
                    Log.e("wew", "shapeView.updateColor() called.")
                } else {
                    Log.e("wew", "shapeView is NULL inside the selected view!")
                }
            }
            else -> {
                Log.e("wew", "Selected view is not of a type that can change color. Type: ${currentView.tag}")
            }
        }

        if (oldColor != null && oldColor != newColor) {
            val action = EditorAction(
                view = currentView,
                actionType = ActionType.CHANGE_COLOR,
                oldColor = oldColor,
                newColor = newColor
            )
            mGraphicManager.pushUndoAction(action)
        }
    }

    override fun changeSelectedViewStrokeWidth(newWidth: Float) {
        val currentView = viewState.currentSelectedView ?: return

        if (currentView.tag is ViewType && currentView.tag == ViewType.BRUSH_DRAWING ||
            currentView.tag is Pair<*, *> && (currentView.tag as Pair<*, *>).first == ViewType.BRUSH_DRAWING) {
            val shapeView = currentView.findViewById<ja.burhanrashid52.photoeditor.shape.ShapeView>(R.id.shape_view)
            if (shapeView != null) {
                val oldWidth = shapeView.getCurrentStrokeWidth()

                if (oldWidth != newWidth) {
                    val action = EditorAction(
                        view = currentView,
                        actionType = ActionType.CHANGE_STROKE,
                        oldStrokeWidth = oldWidth,
                        newStrokeWidth = newWidth
                    )
                    mGraphicManager.pushUndoAction(action)
                }

                shapeView.updateStrokeWidth(newWidth)
            }
        }
    }

    override fun getSelectedViewStrokeWidth(): Float? {
        val currentView = viewState.currentSelectedView ?: return null
        if (currentView.tag is ViewType && currentView.tag == ViewType.BRUSH_DRAWING ||
            currentView.tag is Pair<*, *> && (currentView.tag as Pair<*, *>).first == ViewType.BRUSH_DRAWING) {
                val shapeView = currentView.findViewById<ShapeView>(R.id.shape_view)
                return shapeView?.getCurrentStrokeWidth()
            }
        return null
    }

    override fun changeSelectedViewStrokeStyle(newStyle: StrokeStyle) {
        val currentView = viewState.currentSelectedView ?: return

        if (currentView.tag is ViewType && currentView.tag == ViewType.BRUSH_DRAWING ||
            currentView.tag is Pair<*, *> && (currentView.tag as Pair<*, *>).first == ViewType.BRUSH_DRAWING) {
            val shapeView = currentView.findViewById<ShapeView>(R.id.shape_view)
            if (shapeView != null) {
                val oldStyle = shapeView.getCurrentStrokeStyle()
                if (oldStyle != newStyle) {
                    val action = EditorAction(view = currentView, actionType = ActionType.CHANGE_STROKE_STYLE, oldStrokeStyle =  oldStyle, newStrokeStyle =  newStyle)
                    mGraphicManager.pushUndoAction(action)
                }
                shapeView.updateStrokeStyle(newStyle)
            }
        }
    }

    override fun getSelectedViewStrokeStyle(): StrokeStyle? {
        val currentView = viewState.currentSelectedView ?: return null
        if (currentView.tag is ViewType && currentView.tag == ViewType.BRUSH_DRAWING ||
            currentView.tag is Pair<*, *> && (currentView.tag as Pair<*, *>).first == ViewType.BRUSH_DRAWING) {
            val shapeView = currentView.findViewById<ShapeView>(R.id.shape_view)
            return shapeView?.getCurrentStrokeStyle()
        }
        return null
    }

    override fun onViewAdd(drawingView: DrawingView) {
        mOnPhotoEditorListener?.onAddViewListener(ViewType.BRUSH_DRAWING, viewState.addedViewsCount)
        viewState.addAddedView(drawingView)
    }

    override fun onViewRemoved(drawingView: DrawingView) {
        mOnPhotoEditorListener?.onRemoveViewListener(ViewType.BRUSH_DRAWING, viewState.addedViewsCount)
        viewState.removeAddedView(drawingView)
    }

    override fun onStartDrawing() {
        mOnPhotoEditorListener?.onStartViewChangeListener(ViewType.BRUSH_DRAWING)
    }

    override fun onStopDrawing() {
        mOnPhotoEditorListener?.onStopViewChangeListener(ViewType.BRUSH_DRAWING)
    }

    override fun onShapeCreated(shape: AbstractShape, touchX: Float, touchY: Float) {
        val bounds = shape.bounds
        if (bounds.width() < AbstractShape.TOUCH_TOLERANCE || bounds.height() < AbstractShape.TOUCH_TOLERANCE) return

        val shapeBuilder = drawingView.currentShapeBuilder

        val sourceImageViewTop = imageView.top.toFloat()
        val correctedTop = bounds.top + sourceImageViewTop

        val maxStrokeWidth = 50f

        val shapeGraphic = Shape(photoEditorView, mMultiTouchListener, viewState, mGraphicManager)
        val rootView = shapeGraphic.rootView
        val frmBorder = rootView.findViewById<FrameLayout>(R.id.frmBorder)

        val borderContentWidth = (bounds.width() + maxStrokeWidth).toInt()
        val borderContentHeight = (bounds.height() + maxStrokeWidth).toInt()

        val borderParams = frmBorder.layoutParams as ConstraintLayout.LayoutParams
        borderParams.width = borderContentWidth
        borderParams.height = borderContentHeight
        frmBorder.layoutParams = borderParams

        val translatedPath = Path(shape.path)
        val pathOffsetX = -bounds.left + (maxStrokeWidth / 2f)
        val pathOffsetY = -bounds.top + (maxStrokeWidth / 2f)
        translatedPath.offset(pathOffsetX, pathOffsetY)

        val rootParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        rootView.layoutParams = rootParams // Terapkan sementara untuk pengukuran

        rootView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val measuredWidth = rootView.measuredWidth
        val measuredHeight = rootView.measuredHeight

        val shapeCenterX = bounds.left + bounds.width() / 2f
        val shapeCenterY = correctedTop + bounds.height() / 2f

        rootParams.leftMargin = (shapeCenterX - measuredWidth / 2f).toInt()
        rootParams.topMargin = (shapeCenterY - measuredHeight / 2f).toInt()

        rootView.layoutParams = rootParams

        shapeGraphic.buildView(shapeBuilder, translatedPath)

        addToEditor(shapeGraphic, clearFocus = false)

        // Reset mode drawing.
        drawingView.isShapeCreatingMode = false
        setBrushDrawingMode(false)
        mOnPhotoEditorListener?.onShapeCreated()
    }

    fun enterShapeCreatingMode() {
        Log.d("DrawingView", "Entering Shape Creating Mode...")
        drawingView.isShapeCreatingMode = true
        setBrushDrawingMode(true)
    }

    fun exitAllDrawingModes() {
        Log.d("DrawingView", "exitShapeCreatingMode ${drawingView.isShapeCreatingMode} ${drawingView.isDrawingEnabled}")
        drawingView.isShapeCreatingMode = false
        setBrushDrawingMode(false)
    }

    override fun deleteSelectedView(): Boolean {
        val currentView = viewState.currentSelectedView
        if (currentView != null) {
            if (mGraphicManager.removeView(currentView)) {
                viewToGraphicMap.remove(currentView)
                viewState.clearCurrentSelectedView()
                (context as? EditImageActivity)?.hideDeleteButton()
                return true
            }
        }
        return false
    }

    override fun undo(): Boolean {
        clearHelperBox()

        val undoneView = mGraphicManager.undo()

        if (undoneView != null) {

        }
        return viewState.undoActionsCount > 0
    }

    override fun redo(): Boolean {
        return mGraphicManager.redo()
    }

    override val isUndoAvailable get() = viewState.undoActionsCount > 0
    override val isRedoAvailable get() =  viewState.redoActionsCount > 0

    override fun isAnyViewSelected(): Boolean {
        return viewState.currentSelectedView != null
    }

    override fun onTransform(view: View, oldTransform: ViewTransform, newTransform: ViewTransform) {
        mGraphicManager.pushTransformAction(view, oldTransform, newTransform)
        (context as? EditImageActivity)?.updateActionButtonsState()
    }

    fun repositionAllViews(oldParentWidth: Int, oldParentHeight: Int, newParentWidth: Int, newParentHeight: Int) {
        val views = mGraphicManager.getAllAddedViews()

        for (view in views) {
            val params = view.layoutParams as? RelativeLayout.LayoutParams ?: continue

            val oldCenterX = params.leftMargin + view.width / 2f
            val oldCenterY = params.topMargin + view.height / 2f

            val relativeX = oldCenterX / oldParentWidth
            val relativeY = oldCenterY / oldParentHeight

            val newCenterX = relativeX * newParentWidth
            val newCenterY = relativeY * newParentHeight

            params.leftMargin = (newCenterX - view.width / 2f).toInt()
            params.topMargin = (newCenterY - view.height / 2f).toInt()

            view.layoutParams = params
        }
    }

    override fun setOnPhotoEditorListener(onPhotoEditorListener: OnPhotoEditorListener) {
        mOnPhotoEditorListener = onPhotoEditorListener
        mGraphicManager.onPhotoEditorListener = mOnPhotoEditorListener

        mMultiTouchListener.mOnPhotoEditorListener = onPhotoEditorListener
    }

    init {
        drawingView.setBrushViewChangeListener(this)

        mMultiTouchListener = MultiTouchListener(
            deleteView,
            photoEditorView,
            imageView,
            builder.isTextPinchScalable,
            viewState,
            this
        )

        viewState.deleteView = deleteView
        drawingView.setBrushViewChangeListener(this)
        val mDetector = GestureDetector(
            context,
            PhotoEditorImageViewListener(
                viewState,
                object : OnSingleTapUpCallback {
                    override fun onSingleTapUp() {
                        clearHelperBox()
                        (context as? EditImageActivity)?.hideDeleteButton()
                    }
                }
            )
        )

        imageView.setOnTouchListener { _, event ->
            mOnPhotoEditorListener?.onTouchSourceImage(event)
            mDetector.onTouchEvent(event)
        }
        photoEditorView.setClipSourceImage(builder.clipSourceImage)
    }
}
