package ja.burhanrashid52.photoediting

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.*
import ja.burhanrashid52.photoediting.EmojiBSFragment.EmojiListener
import ja.burhanrashid52.photoediting.StickerBSFragment.StickerListener
import ja.burhanrashid52.photoediting.base.BaseActivity
import ja.burhanrashid52.photoediting.constant.ResponseCode
import ja.burhanrashid52.photoediting.filters.FilterListener
import ja.burhanrashid52.photoediting.filters.FilterViewAdapter
import ja.burhanrashid52.photoediting.tools.EditingToolsAdapter
import ja.burhanrashid52.photoediting.tools.EditingToolsAdapter.OnItemSelected
import ja.burhanrashid52.photoediting.tools.ToolType
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorImpl
import ja.burhanrashid52.photoeditor.PhotoEditorView
import ja.burhanrashid52.photoeditor.PhotoFilter
import ja.burhanrashid52.photoeditor.R
import ja.burhanrashid52.photoeditor.SaveFileResult
import ja.burhanrashid52.photoeditor.SaveSettings
import ja.burhanrashid52.photoeditor.TextStyleBuilder
import ja.burhanrashid52.photoeditor.ViewType
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class EditImageActivity : BaseActivity(), OnPhotoEditorListener, View.OnClickListener,
    PropertiesBSFragment.Properties, ShapeBSFragment.Properties, EmojiListener, StickerListener,
    OnItemSelected, FilterListener {

    lateinit var mPhotoEditor: PhotoEditor
    private lateinit var mPhotoEditorView: PhotoEditorView
    private lateinit var mPropertiesBSFragment: PropertiesBSFragment
    private lateinit var mShapeBSFragment: ShapeBSFragment
    private val mShapeBuilder = ShapeBuilder()
    private lateinit var mEmojiBSFragment: EmojiBSFragment
    private lateinit var mStickerBSFragment: StickerBSFragment
    private lateinit var mTxtCurrentTool: TextView
    private lateinit var mWonderFont: Typeface
    private lateinit var mRvTools: RecyclerView
    private lateinit var mRvFilters: RecyclerView
    private lateinit var mImgUndo: View
    private lateinit var mImgRedo: View
    private lateinit var mImgDelete: View
    private lateinit var mImgDuplicate: View
    private lateinit var mImgPalette: View
    private val mEditingToolsAdapter = EditingToolsAdapter(this)
    private val mFilterViewAdapter = FilterViewAdapter(this)
    private lateinit var mRootView: ConstraintLayout
    private val mConstraintSet = ConstraintSet()
    private var mIsFilterVisible = false
    private var isModule = true
    private var sourceUri: Uri? = null
    var originalBitmap: Bitmap? = null
    var currentPhotoFilter: PhotoFilter = PhotoFilter.NONE

    private var lastPhotoEditorWidth = 0
    private var lastPhotoEditorHeight = 0

    @VisibleForTesting
    var mSaveImageUri: Uri? = null

    private lateinit var mSaveFileHelper: FileSaveHelper

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Tunda sedikit agar PhotoEditorView sempat diukur ulang oleh sistem
        // untuk dimensi landscape/portrait yang baru.
        Handler(Looper.getMainLooper()).postDelayed({
            val newWidth = mPhotoEditorView.width
            val newHeight = mPhotoEditorView.height

            // Jangan lakukan apa-apa jika dimensi belum siap atau tidak berubah
            if (lastPhotoEditorWidth == 0 || lastPhotoEditorHeight == 0 || (newWidth == lastPhotoEditorWidth && newHeight == lastPhotoEditorHeight)) {
                return@postDelayed
            }

            // Dapatkan semua view yang ditambahkan dari PhotoEditor
            // Ini memerlukan sedikit perubahan pada PhotoEditor/GraphicManager
            (mPhotoEditor as? PhotoEditorImpl)?.repositionAllViews(lastPhotoEditorWidth, lastPhotoEditorHeight, newWidth, newHeight)

            // Update dimensi terakhir
            lastPhotoEditorWidth = newWidth
            lastPhotoEditorHeight = newHeight

        }, 100) // Tundaan 100ms biasanya cukup
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeFullScreen()
        setContentView(R.layout.activity_edit_image)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()

        handleIntentImage(mPhotoEditorView.source)

        mWonderFont = Typeface.createFromAsset(assets, "beyond_wonderland.ttf")

        mPropertiesBSFragment = PropertiesBSFragment()
        mEmojiBSFragment = EmojiBSFragment()
        mStickerBSFragment = StickerBSFragment()
        mShapeBSFragment = ShapeBSFragment()
        mStickerBSFragment.setStickerListener(this)
        mEmojiBSFragment.setEmojiListener(this)
        mPropertiesBSFragment.setPropertiesChangeListener(this)
        mShapeBSFragment.setPropertiesChangeListener(this)

        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvTools.layoutManager = llmTools
        mRvTools.adapter = mEditingToolsAdapter

        val llmFilters = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvFilters.layoutManager = llmFilters
        mRvFilters.adapter = mFilterViewAdapter

        // NOTE(lucianocheng): Used to set integration testing parameters to PhotoEditor
        val pinchTextScalable = intent.getBooleanExtra(PINCH_TEXT_SCALABLE_INTENT_KEY, true)

        // val mTextRobotoTf = ResourcesCompat.getFont(this, R.font.roboto_medium)
        // val mEmojiTypeFace = Typeface.createFromAsset(getAssets(), "emojione-android.ttf")

        mPhotoEditor = PhotoEditor.Builder(this, mPhotoEditorView)
            .setPinchTextScalable(pinchTextScalable) // set flag to make text scalable when pinch
            //.setDefaultTextTypeface(mTextRobotoTf)
            //.setDefaultEmojiTypeface(mEmojiTypeFace)
            .build() // build photo editor sdk

        mPhotoEditor.setOnPhotoEditorListener(this)

        val value = intent.extras
        val path = value?.getString("path")
        var tools = arrayOf("draw", "clip",
            "imageSticker",
            "textSticker",
            "mosaic",
            "filter",
            "adjust",
            "line",
            "arrow",
            "square",
            "circle", "pointer")

        value?.getStringArray("tools")?.let {
            tools = it
        }

        initTools(tools)

        if (path != null) {
            Glide.with(this)
                .asBitmap()
                .load(path)
                .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {

                        originalBitmap = resource
                        mPhotoEditorView.source.setImageBitmap(resource)
                        sourceUri = getImageUri(resource)
                    }
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        // Tangani kasus gagal muat di sini
                        Log.e("EditImageActivity", "Glide failed to load image from path: $path")
                        if (isModule) {
                            val intent = Intent()
                            intent.putExtra("path", path)
                            setResult(ResponseCode.LOAD_IMAGE_FAILED, intent)
                            finish()
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        TODO("Not yet implemented")
                    }
                })
        } else {
            isModule = false

            val defaultDrawable = ContextCompat.getDrawable(applicationContext, R.drawable.paris_tower)

            if (defaultDrawable != null) {
                val defaultBitmap = drawableToBitmap(defaultDrawable)
                originalBitmap = defaultBitmap
                mPhotoEditorView.source.setImageBitmap(defaultBitmap)

                sourceUri = getImageUri(defaultBitmap)
            } else {
                showSnackbar("Failed to load default image.")
            }
        }

        mSaveFileHelper = FileSaveHelper(this)

        mPhotoEditorView.post {
            lastPhotoEditorWidth = mPhotoEditorView.width
            lastPhotoEditorHeight = mPhotoEditorView.height
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        val bitmap: Bitmap
        if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun updateActionButtonsState() {
        mImgUndo.isEnabled = mPhotoEditor.isUndoAvailable
        mImgRedo.isEnabled = mPhotoEditor.isRedoAvailable

        mImgDelete.isEnabled =  mPhotoEditor.isAnyViewSelected()
        mImgDuplicate.isEnabled =  mPhotoEditor.isAnyViewSelected()
//        mImgPalette.isEnabled =  mPhotoEditor.isAnyViewSelected()
    }

    private fun handleIntentImage(source: ImageView) {
        if (intent == null) {
            return
        }

        when (intent.action) {
            Intent.ACTION_EDIT, ACTION_NEXTGEN_EDIT -> {
                try {
                    val uri = intent.data
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    source.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            else -> {
                val intentType = intent.type
                if (intentType != null && intentType.startsWith("image/")) {
                    val imageUri = intent.data
                    if (imageUri != null) {
                        source.setImageURI(imageUri)
                    }
                }
            }
        }
    }

    private fun initViews() {
        mPhotoEditorView = findViewById(R.id.photoEditorView)
        mTxtCurrentTool = findViewById(R.id.txtCurrentTool)
        mRvTools = findViewById(R.id.rvConstraintTools)
        mRvFilters = findViewById(R.id.rvFilterView)
        mRootView = findViewById(R.id.rootView)

        mImgUndo = findViewById(R.id.imgUndo)
        mImgUndo.setOnClickListener(this)
        mImgUndo.isEnabled = false

        mImgRedo = findViewById(R.id.imgRedo)
        mImgRedo.setOnClickListener(this)
        mImgRedo.isEnabled = false

        mImgDelete = findViewById(R.id.imgRemove)
        mImgDelete.setOnClickListener(this)
        mImgDelete.isEnabled = false

        mImgDuplicate = findViewById(R.id.imgDuplicate)
        mImgDuplicate.setOnClickListener(this)
        mImgDuplicate.isEnabled = false

        mImgPalette = findViewById(R.id.imgPalette)
        mImgPalette.setOnClickListener(this)
        mImgPalette.isEnabled = false

        val imgCamera: ImageView = findViewById(R.id.imgCamera)
        imgCamera.setOnClickListener(this)

        val imgGallery: ImageView = findViewById(R.id.imgGallery)
        imgGallery.setOnClickListener(this)

        val btnDone: Button = findViewById(R.id.btnDone)
        btnDone.setOnClickListener(this)

        val btnCancel: Button = findViewById(R.id.btnCancel)
        btnCancel.setOnClickListener(this)

        val imgShare: ImageView = findViewById(R.id.imgShare)
        imgShare.setOnClickListener(this)
    }

    private fun initTools(tools: Array<String>)  {
        if ("pointer" in tools) {
            mEditingToolsAdapter.addTool("pointer")
        }
        if ("draw" in tools || "line" in tools || "square" in tools || "circle" in tools || "arrow" in tools) {
            mEditingToolsAdapter.addTool("shape")

            // handle display shape here
            if ("draw" in tools) {
                mShapeBSFragment.addShape("draw")
            }
            if ("line" in tools) {
                mShapeBSFragment.addShape("line")
            }
            if ("arrow" in tools) {
                mShapeBSFragment.addShape("arrow")
            }
            if ("square" in tools) {
                mShapeBSFragment.addShape("rect")
            }
            if ("circle" in tools) {
                mShapeBSFragment.addShape("oval")
            }
        }

        if ("clip" in tools) {
            mEditingToolsAdapter.addTool("clip")
        }

        if ("textSticker" in tools) {
            mEditingToolsAdapter.addTool("text")
        }

        if ("imageSticker" in tools) {
            mEditingToolsAdapter.addTool("sticker")
        }

        if ("filter" in tools) {
            mEditingToolsAdapter.addTool("filter")
        }
    }

    override fun onEditTextChangeListener(rootView: View, text: String, colorCode: Int, backgroundColor: Int) {
        val textView = rootView.findViewById<TextView>(R.id.tvPhotoEditorText)
        val currentTextSize = textView.textSize / resources.displayMetrics.scaledDensity

        Log.d("TextGraphicDebug", "Listener in Activity received: text='$text', view hash=${rootView.hashCode()}")

        val textEditorDialogFragment = TextEditorDialogFragment.show(this, text, colorCode, backgroundColor, currentTextSize)
        textEditorDialogFragment.setOnTextEditorListener(object :
            TextEditorDialogFragment.TextEditorListener {
            override fun onDone(inputText: String, textColor: Int, backgroundColor: Int, textSize: Float) {
                val styleBuilder = TextStyleBuilder()
                styleBuilder.withTextColor(textColor)
                styleBuilder.withBackgroundColor(backgroundColor)
                styleBuilder.withTextSize(textSize)
                mPhotoEditor.editText(rootView, inputText, styleBuilder)
                mTxtCurrentTool.setText(R.string.label_text)
                mEditingToolsAdapter.selectTool(ToolType.POINTER)
            }

            override fun onCancel() {
                mEditingToolsAdapter.selectTool(ToolType.POINTER)
            }

        })
    }

    override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
        Log.d(
            TAG,
            "onAddViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )

        updateActionButtonsState()
        if (viewType == ViewType.BRUSH_DRAWING) {
            mImgPalette.isEnabled = true
        } else {
            mImgPalette.isEnabled = false
        }
    }

    override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
        Log.d(
            TAG,
            "onRemoveViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )

        updateActionButtonsState()
        if (viewType == ViewType.BRUSH_DRAWING) {
            mImgPalette.isEnabled = true
        } else {
            mImgPalette.isEnabled = false
        }
    }

    override fun onStartViewChangeListener(viewType: ViewType) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [$viewType]")
        mImgDelete.isEnabled = true
        mImgDuplicate.isEnabled = true

        if (viewType == ViewType.BRUSH_DRAWING) {
            mImgPalette.isEnabled = true
        } else {
            mImgPalette.isEnabled = false
        }
    }

    override fun onStopViewChangeListener(viewType: ViewType) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onTouchSourceImage(event: MotionEvent) {
        Log.d(TAG, "onTouchView() called with: event = [$event]")
    }

    override fun onShapeCreated() {
        mEditingToolsAdapter.selectTool(ToolType.POINTER)
    }

    @SuppressLint("NonConstantResourceId", "MissingPermission")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgUndo -> {
                mPhotoEditor.undo()
                updateActionButtonsState()
            }

            R.id.imgRedo -> {
                mPhotoEditor.redo()
                updateActionButtonsState()
            }

            R.id.imgRemove -> {
                mPhotoEditor.deleteSelectedView()
                updateActionButtonsState()
            }

            R.id.imgDuplicate -> {
                mPhotoEditor.duplicateSelectedView()
                updateActionButtonsState()
            }
            R.id.imgPalette -> {
                val currentStroke = mPhotoEditor.getSelectedViewStrokeWidth()
                    ?: TopPaletteDialogFragment.STROKE_MEDIUM
                val currentStyle = mPhotoEditor.getSelectedViewStrokeStyle()
                    ?: StrokeStyle.SOLID

                val topSheet = TopPaletteDialogFragment.newInstance(currentStroke, currentStyle)
                topSheet.setOnColorSelectListener { color ->
                    mPhotoEditor.changeSelectedViewColor(color)
                }
                topSheet.setOnStrokeWidthSelectListener { width ->
                    mPhotoEditor.changeSelectedViewStrokeWidth(width)
                }
                topSheet.setOnStrokeStyleSelectListener { style ->
                    mPhotoEditor.changeSelectedViewStrokeStyle(style)
                }
                topSheet.show(supportFragmentManager, "ColorPickerTopSheet")
            }

            R.id.btnDone -> saveImage()
            R.id.btnCancel -> onBackPressed()
            R.id.imgShare -> shareImage()
            R.id.imgCamera -> {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)
            }

            R.id.imgGallery -> {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST)
            }
        }
    }

    private fun shareImage() {
        val saveImageUri = mSaveImageUri
        if (saveImageUri == null) {
            showSnackbar(getString(R.string.msg_save_image_to_share))
            return
        }

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, buildFileProviderUri(saveImageUri))
        startActivity(Intent.createChooser(intent, getString(R.string.msg_share_image)))
    }

    private fun buildFileProviderUri(uri: Uri): Uri {
        if (FileSaveHelper.isSdkHigherThan28()) {
            return uri
        }
        val path: String = uri.path ?: throw IllegalArgumentException("URI Path Expected")

        return FileProvider.getUriForFile(
            this,
            FILE_PROVIDER_AUTHORITY,
            File(path)
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    private fun saveImage() {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val hasStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasStoragePermission || FileSaveHelper.isSdkHigherThan28()) {
            showLoading("Saving...")
            val value = intent.extras
            val targetPath = value?.getString("targetPath")
            mSaveFileHelper.createFile(fileName, targetPath, object : FileSaveHelper.OnFileCreateResult {

                @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
                override fun onFileCreateResult(
                    created: Boolean,
                    filePath: String?,
                    error: String?,
                    uri: Uri?
                ) {
                    lifecycleScope.launch {
                        if (created && filePath != null) {
                            val saveSettings = SaveSettings.Builder()
                                .setClearViewsEnabled(true)
                                .setTransparencyEnabled(true)
                                .build()

                            val result = mPhotoEditor.saveAsFile(filePath, saveSettings)

                            if (result is SaveFileResult.Success) {
                                hideLoading()

                                if (isModule) {
                                    val intent = Intent()
                                    intent.putExtra("path", filePath)
                                    setResult(ResponseCode.RESULT_OK, intent)
                                    finish()
                                } else {
                                    mSaveFileHelper.notifyThatFileIsNowPubliclyAvailable(contentResolver)
                                    showSnackbar("Image Saved Successfully")
                                    mSaveImageUri = uri
                                    mPhotoEditorView.source.setImageURI(mSaveImageUri)
                                }
                            } else {
                                hideLoading()
                                if (isModule) {
                                    val intent = Intent()
                                    intent.putExtra("path", filePath)
                                    setResult(ResponseCode.FAILED_TO_SAVE, intent)
                                    finish()
                                }
                                showSnackbar("Failed to save Image")
                            }
                        } else {
                            hideLoading()
                            error?.let { showSnackbar(error) }
                        }
                    }
                }
            })
        } else {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // TODO(lucianocheng): Replace onActivityResult with Result API from Google
    //                     See https://developer.android.com/training/basics/intents/result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mEditingToolsAdapter.selectTool(ToolType.POINTER)
        if (resultCode == RESULT_OK) {
            Log.d("RESULT", resultCode.toString())
            Log.d("REQUEST", requestCode.toString())
            when (requestCode) {
                CAMERA_REQUEST -> {
                    mPhotoEditor.clearAllViews()
                    val photo = data?.extras?.get("data") as Bitmap?
                    mPhotoEditorView.source.setImageBitmap(photo)
                }
                REQUEST_CROP -> {
                    val resultUri = data?.let { getOutput(it) }
                    if (resultUri != null) {
                        try {
                            val newBitmap = MediaStore.Images.Media.getBitmap(contentResolver, resultUri)

//                            val oldBitmap = bitmapBeforeCrop
                            val oldBitmap = originalBitmap

                            if (oldBitmap != null) {
                                mPhotoEditor.addCropAction(oldBitmap, newBitmap)
                            }

                            mPhotoEditorView.source.setImageBitmap(newBitmap)
                            mEditingToolsAdapter.selectTool(ToolType.POINTER)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            showSnackbar("Failed to load cropped image")
                        }
                    }
                }
                PICK_REQUEST -> try {
                    mPhotoEditor.clearAllViews()
                    val uri = data?.data
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
                    mPhotoEditorView.source.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onColorChanged(colorCode: Int) {
        mShapeBuilder.withShapeColor(colorCode)
        mPhotoEditor.setShape(mShapeBuilder)
        mTxtCurrentTool.setText(R.string.label_brush)
    }

    override fun onOpacityChanged(opacity: Int) {
        mShapeBuilder.withShapeOpacity(opacity)
        mPhotoEditor.setShape(mShapeBuilder.withShapeOpacity(opacity))
        mTxtCurrentTool.setText(R.string.label_brush)
    }

    override fun onShapeSizeChanged(shapeSize: Int) {
        mShapeBuilder.withShapeSize(shapeSize.toFloat())
        mPhotoEditor.setShape(mShapeBuilder.withShapeSize(shapeSize.toFloat()))
        mTxtCurrentTool.setText(R.string.label_brush)
    }

    override fun onShapePicked(shapeType: ShapeType) {
        mShapeBuilder.withShapeType(shapeType)
        mPhotoEditor.setShape(mShapeBuilder)
    }

    override fun onEmojiClick(emojiUnicode: String) {
        mPhotoEditor.addEmoji(emojiUnicode)
        mTxtCurrentTool.setText(R.string.label_emoji)
    }

    override fun onStickerClick(bitmap: Bitmap) {
        mPhotoEditor.addImage(bitmap)
        mTxtCurrentTool.setText(R.string.label_sticker)
    }

    @SuppressLint("MissingPermission")
    override fun isPermissionGranted(isGranted: Boolean, permission: String?) {
        if (isGranted) {
            saveImage()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.msg_save_image))
        builder.setPositiveButton("Save") { _: DialogInterface?, _: Int -> saveImage() }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        builder.setNeutralButton("Discard") { _: DialogInterface?, _: Int -> finish() }
        builder.create().show()
    }

    override fun onFilterSelected(photoFilter: PhotoFilter) {
//        mPhotoEditor.setFilterEffect(photoFilter)
        Log.e("wew", "filter: ${photoFilter.name} ori:  ${currentPhotoFilter.name}")
        if (currentPhotoFilter == photoFilter) return

        val oldFilter = currentPhotoFilter
        val newFilter = photoFilter

        mPhotoEditor.addFilterAction(oldFilter, newFilter)

        originalBitmap?.let { mPhotoEditor.setFilterEffect(it, newFilter) }

        currentPhotoFilter = newFilter
        mImgUndo.isEnabled = true
    }

    override fun onToolSelected(toolType: ToolType) {

        if (toolType != ToolType.SHAPE) {
            (mPhotoEditor as PhotoEditorImpl).exitAllDrawingModes()
        } else{
            (mPhotoEditor as PhotoEditorImpl).enterShapeCreatingMode()
        }

        when (toolType) {
            ToolType.SHAPE -> {
                Log.d("DrawingView", "masuk")
                mTxtCurrentTool.setText(R.string.label_shape)
                showBottomSheetDialogFragment(mShapeBSFragment)
                showFilter(false)
            }

            ToolType.TEXT -> {
                val textEditorDialogFragment = TextEditorDialogFragment.show(this)
                textEditorDialogFragment.setOnTextEditorListener(object :
                    TextEditorDialogFragment.TextEditorListener {
                    // Update signature onDone
                    override fun onDone(inputText: String, textColor: Int, backgroundColor: Int, textSize: Float) {
                        val styleBuilder = TextStyleBuilder()
                        styleBuilder.withTextColor(textColor)
                        styleBuilder.withBackgroundColor(backgroundColor)
                        styleBuilder.withTextSize(textSize)
                        mPhotoEditor.addText(inputText, styleBuilder)
                        mTxtCurrentTool.setText(R.string.label_text)
                        mEditingToolsAdapter.selectTool(ToolType.POINTER)
                    }

                    override fun onCancel() {
                        mEditingToolsAdapter.selectTool(ToolType.POINTER)
                    }
                })
                showFilter(false)
            }

            ToolType.FILTER -> {
                mTxtCurrentTool.setText(R.string.label_filter)
                showFilter(true)
            }

            ToolType.EMOJI -> {
                showBottomSheetDialogFragment(mEmojiBSFragment)
                showFilter(false)

            }
            ToolType.STICKER -> {
                showBottomSheetDialogFragment(mStickerBSFragment)
                showFilter(false)
            }
            ToolType.CLIP -> {
//                bitmapBeforeCrop = (mPhotoEditorView.source.drawable as? BitmapDrawable)?.bitmap
                sourceUri?.let {
                    val options =
                        Options()
                    options.setFreeStyleCropEnabled (true)
                    of(it, it)
                        .withMaxResultSize(2048, 2048)
                        .withOptions(options)
                        .start(this, UCrop.REQUEST_CROP)
                };
                showFilter(false)
            }

            ToolType.POINTER -> {
                mTxtCurrentTool.setText(R.string.label_pointer)
                showFilter(false)
            }
        }
    }

    private fun showBottomSheetDialogFragment(fragment: BottomSheetDialogFragment?) {
        Log.d("DrawingView", "masuk 1")
        if (fragment == null) {
            return
        }
        Log.d("DrawingView", "masuk 2")
        fragment.show(supportFragmentManager, fragment.tag)
    }

    private fun showFilter(isVisible: Boolean) {
        mIsFilterVisible = isVisible
        mConstraintSet.clone(mRootView)

        val rvFilterId: Int = mRvFilters.id

        if (isVisible) {
            mConstraintSet.clear(rvFilterId, ConstraintSet.START)
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START
            )
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
        } else {
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
            mConstraintSet.clear(rvFilterId, ConstraintSet.END)
        }

        val changeBounds = ChangeBounds()
        changeBounds.duration = 350
        changeBounds.interpolator = AnticipateOvershootInterpolator(1.0f)
        TransitionManager.beginDelayedTransition(mRootView, changeBounds)

        mConstraintSet.applyTo(mRootView)
    }

    override fun onBackPressed() {
       if (!mPhotoEditor.isCacheEmpty) {
            showSaveDialog()
        } else {
            if (isModule) {
                val intent = Intent()
                setResult(ResponseCode.RESULT_CANCELED, intent)
                finish()
            } else {
                super.onBackPressed()
            }
        }
    }

    fun getImageUri(image: Drawable?): Uri {
        val bitmap = (image as BitmapDrawable).bitmap
        val context = applicationContext;
        val file = File(context.cacheDir, "image.png")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()
        return file.toUri()
    }

    fun getImageUri(bitmap: Bitmap): Uri {
        val context = applicationContext
        val file = File(context.cacheDir, "source_image_${System.currentTimeMillis()}.png")
        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file.toUri()
    }

    fun hideDeleteButton() {
        mImgDelete.isEnabled = false
        mImgDuplicate.isEnabled = false
        mImgPalette.isEnabled = false
    }

    companion object {

        private const val TAG = "EditImageActivity"

        const val FILE_PROVIDER_AUTHORITY = "com.burhanrashid52.photoediting.fileprovider"
        private const val CAMERA_REQUEST = 52
        private const val PICK_REQUEST = 53
        const val ACTION_NEXTGEN_EDIT = "action_nextgen_edit"
        const val PINCH_TEXT_SCALABLE_INTENT_KEY = "PINCH_TEXT_SCALABLE"
    }

}