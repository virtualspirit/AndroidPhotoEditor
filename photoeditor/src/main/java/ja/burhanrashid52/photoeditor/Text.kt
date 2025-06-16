package ja.burhanrashid52.photoeditor

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView

/**
 * Created by Burhanuddin Rashid on 14/05/21.
 *
 * @author <https:></https:>//github.com/burhanrashid52>
 */
class Text(
    private val mPhotoEditorView: PhotoEditorView,
    private val mMultiTouchListener: MultiTouchListener,
    private val mViewState: PhotoEditorViewState,
    private val mDefaultTextTypeface: Typeface?,
    private val mGraphicManager: GraphicManager
) : Graphic(
    context = mPhotoEditorView.context,
    graphicManager = mGraphicManager,
    viewType = ViewType.TEXT,
    layoutId = R.layout.view_photo_editor_text
) {

    private var mTextView: TextView? = null

    fun buildView(text: String?, styleBuilder: TextStyleBuilder?) {
        mTextView?.apply {
            this.text = text
            styleBuilder?.applyStyle(this)
        }
    }

    private fun setupGesture() {

        val onGestureControl = object : MultiTouchListener.OnGestureControl {
            override fun onClick() {
                val boxHelper = BoxHelper(mPhotoEditorView, mViewState)
                boxHelper.clearHelperBox()
                toggleSelection()
                mViewState.currentSelectedView = rootView

                val textInput = mTextView?.text.toString()
                val currentTextColor = mTextView?.currentTextColor ?: 0
                val currentBackgroundColor = (mTextView?.background as? ColorDrawable)?.color ?: Color.TRANSPARENT

                mGraphicManager.onPhotoEditorListener?.onEditTextChangeListener(
                    rootView,
                    textInput,
                    currentTextColor,
                    currentBackgroundColor
                )
            }

            override fun onLongClick() {
            }
        }

        mMultiTouchListener.setOnGestureControl(onGestureControl)
        val rootView = rootView
        rootView.setOnTouchListener(mMultiTouchListener)
    }

    override fun setupView(rootView: View) {
        mTextView = rootView.findViewById(R.id.tvPhotoEditorText)
        mTextView?.run {
            gravity = Gravity.CENTER
            typeface = mDefaultTextTypeface
        }
    }

    init {
        setupGesture()
    }
}