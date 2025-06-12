package ja.burhanrashid52.photoeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.Typeface
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.annotation.RequiresPermission
import ja.burhanrashid52.photoediting.EditImageActivity
import ja.burhanrashid52.photoeditor.PhotoEditorImageViewListener.OnSingleTapUpCallback
import ja.burhanrashid52.photoeditor.shape.AbstractShape
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
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
) : PhotoEditor, BrushViewChangeListener {
    private val photoEditorView: PhotoEditorView = builder.photoEditorView
    private val viewState: PhotoEditorViewState = PhotoEditorViewState()
    private val imageView: ImageView = builder.imageView
    private val deleteView: View? = builder.deleteView
    private val drawingView: DrawingView = builder.drawingView
    private val mBrushDrawingStateListener: BrushDrawingStateListener =
        BrushDrawingStateListener(builder.photoEditorView, viewState)
    private val mBoxHelper: BoxHelper = BoxHelper(builder.photoEditorView, viewState)
    private var mOnPhotoEditorListener: OnPhotoEditorListener? = null
    private val isTextPinchScalable: Boolean = builder.isTextPinchScalable
    private val mDefaultTextTypeface: Typeface? = builder.textTypeface
    private val mDefaultEmojiTypeface: Typeface? = builder.emojiTypeface
    private val mGraphicManager: GraphicManager = GraphicManager(builder.photoEditorView, viewState)
    private val context: Context = builder.context

    override fun addImage(desiredImage: Bitmap) {
        val multiTouchListener = getMultiTouchListener(true)
        val sticker = Sticker(photoEditorView, multiTouchListener, viewState, mGraphicManager)
        sticker.buildView(desiredImage)
        addToEditor(sticker)
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

    override fun addText(text: String, styleBuilder: TextStyleBuilder?) {
        drawingView.enableDrawing(false)
        val multiTouchListener = getMultiTouchListener(isTextPinchScalable)
        val textGraphic = Text(
            photoEditorView,
            multiTouchListener,
            viewState,
            mDefaultTextTypeface,
            mGraphicManager
        )
        textGraphic.buildView(text, styleBuilder)
        addToEditor(textGraphic)
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

    private fun addToEditor(graphic: Graphic) {
//        clearHelperBox()
//        mGraphicManager.addView(graphic)
//        // Change the in-focus view
//        viewState.currentSelectedView = graphic.rootView

        // Hapus border dari view yang mungkin sedang fokus
        clearHelperBox()

        mGraphicManager.addView(graphic)

        // Jadikan graphic yang baru ditambahkan sebagai view yang sedang terpilih
        viewState.currentSelectedView = graphic.rootView

        // Secara manual panggil method untuk menampilkan border/handle-nya
        // Kita bisa mengekstrak logika ini dari MultiTouchListener atau Graphic
        graphic.toggleSelection() // Kita perlu membuat method ini public/internal di Graphic.kt
    }

    /**
     * Create a new instance and scalable touchview
     *
     * @param isPinchScalable true if make pinch-scalable, false otherwise.
     * @return scalable multitouch listener
     */
    private fun getMultiTouchListener(isPinchScalable: Boolean): MultiTouchListener {
        return MultiTouchListener(
            deleteView,
            photoEditorView,
            imageView,
            isPinchScalable,
            mOnPhotoEditorListener,
            viewState
        )
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

    override fun setOnPhotoEditorListener(onPhotoEditorListener: OnPhotoEditorListener) {
        mOnPhotoEditorListener = onPhotoEditorListener
        mGraphicManager.onPhotoEditorListener = mOnPhotoEditorListener
        mBrushDrawingStateListener.setOnPhotoEditorListener(mOnPhotoEditorListener)
    }

    override val isCacheEmpty: Boolean
        get() = !isUndoAvailable && !isRedoAvailable

    // region Shape
    override fun setShape(shapeBuilder: ShapeBuilder) {
        drawingView.currentShapeBuilder = shapeBuilder
    } // endregion

    // Implementasikan semua metode dari BrushViewChangeListener
    override fun onViewAdd(drawingView: DrawingView) {
        // Logika untuk brush mode
        mOnPhotoEditorListener?.onAddViewListener(ViewType.BRUSH_DRAWING, viewState.addedViewsCount)
        viewState.addAddedView(drawingView)
    }

    override fun onViewRemoved(drawingView: DrawingView) {
        // Logika untuk brush mode
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

        // Gunakan float untuk presisi lebih tinggi, dan tambahkan safety margin
        val halfStrokeWidth = shapeBuilder.shapeSize / 2f
        val safetyMargin = 2 // 2 piksel ekstra untuk keamanan

        val translatedPath = Path(shape.path)
        translatedPath.offset(-bounds.left + halfStrokeWidth, -bounds.top + halfStrokeWidth)

        val multiTouchListener = getMultiTouchListener(true)
        val shapeGraphic = Shape(photoEditorView, multiTouchListener, viewState, mGraphicManager)

        shapeGraphic.buildView(shapeBuilder, translatedPath)

        // Gunakan Math.ceil untuk pembulatan ke atas
        val newWidth = Math.ceil((bounds.width() + shapeBuilder.shapeSize).toDouble()).toInt() + safetyMargin
        val newHeight = Math.ceil((bounds.height() + shapeBuilder.shapeSize).toDouble()).toInt() + safetyMargin

        val params = RelativeLayout.LayoutParams(
            newWidth.coerceAtLeast(1),
            newHeight.coerceAtLeast(1)
        )

        params.leftMargin = (bounds.left - halfStrokeWidth).toInt() - (safetyMargin / 2)
        params.topMargin = (bounds.top - halfStrokeWidth).toInt() - (safetyMargin / 2)
//
//        params.leftMargin = touchX.toInt()
//        params.topMargin = touchY.toInt()

        Log.e("DrawingView", "Shape 1: ${bounds.top} ${shapeBuilder.shapeSize} $safetyMargin")
        Log.e("DrawingView", "Shape params: ${params.leftMargin} ${params.topMargin}")

        shapeGraphic.rootView.layoutParams = params

        addToEditor(shapeGraphic)

        drawingView.isShapeCreatingMode = false
        setBrushDrawingMode(false)

        mOnPhotoEditorListener?.onShapeCreated()
    }

    fun enterShapeCreatingMode() {
        Log.d("DrawingView", "Entering Shape Creating Mode...")
        drawingView.isShapeCreatingMode = true
        setBrushDrawingMode(true) // Ini akan memanggil enableDrawing(true) di DrawingView
    }

    fun exitAllDrawingModes() {
        Log.d("DrawingView", "exitShapeCreatingMode ${drawingView.isShapeCreatingMode} ${drawingView.isDrawingEnabled}")
        drawingView.isShapeCreatingMode = false
        setBrushDrawingMode(false)
    }

//    override fun deleteSelectedView(): Boolean {
//        val currentView = viewState.currentSelectedView
//        if (currentView != null) {
//            if (mGraphicManager.removeViewBy(currentView)) {
//                viewState.clearCurrentSelectedView()
//                // Setelah menghapus, sembunyikan tombol hapus
//                (context as? EditImageActivity)?.hideDeleteButton()
//                return true
//            }
//        }
//        return false
//    }

    override fun deleteSelectedView(): Boolean {
        val currentView = viewState.currentSelectedView
        if (currentView != null) {
            if (mGraphicManager.removeView(currentView)) {
                viewState.clearCurrentSelectedView()
                (context as? EditImageActivity)?.hideDeleteButton()
                return true
            }
        }
        return false
    }

    override fun undo(): Boolean {
        return mGraphicManager.undo()
    }

    override fun redo(): Boolean {
        return mGraphicManager.redo()
    }

    override val isUndoAvailable get() = viewState.undoActionsCount > 0
    override val isRedoAvailable get() =  viewState.redoActionsCount > 0

//    override fun undo(): Boolean {
//        return mGraphicManager.undoView()
//    }
//
//    override val isUndoAvailable get() = viewState.addedViewsCount > 0
//
//    override fun redo(): Boolean {
//        return mGraphicManager.redoView()
//    }
//
//    override val isRedoAvailable get() = mGraphicManager.redoStackCount > 0

    init {
        viewState.deleteView = deleteView
        drawingView.setBrushViewChangeListener(this)
        val mDetector = GestureDetector(
            context,
            PhotoEditorImageViewListener(
                viewState,
                object : OnSingleTapUpCallback {
                    override fun onSingleTapUp() {
                        clearHelperBox()
                        // Saat helper box dibersihkan (karena tap di area kosong),
                        // sembunyikan juga tombol hapus.
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