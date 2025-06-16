package ja.burhanrashid52.photoediting

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ja.burhanrashid52.photoediting.ColorPickerAdapter.OnColorPickerClickListener
import ja.burhanrashid52.photoeditor.R

/**
 * Created by Burhanuddin Rashid on 1/16/2018.
 */
class TextEditorDialogFragment : DialogFragment(), SeekBar.OnSeekBarChangeListener {

    private lateinit var mAddTextEditText: EditText
    private lateinit var mAddTextDoneBtn: Button
    private lateinit var mAddTextCancelBtn: Button
    private lateinit var mInputMethodManager: InputMethodManager
    private var mTextColor = 0
    private var mBackgroundColor = Color.TRANSPARENT
    private var mTextSize = 0f
    private var mTextEditorListener: TextEditorListener? = null
    private lateinit var tooltipTextView: TextView

    private val MIN_TEXT_SIZE = 12
    private val MAX_TEXT_SIZE = 72

    interface TextEditorListener {
        fun onDone(inputText: String, textColor: Int, backgroundColor: Int, textSize: Float)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        //Make dialog full screen with transparent background
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_text_dialog, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        tooltipTextView = view.findViewById(R.id.tv_tooltip)
        mAddTextEditText = view.findViewById(R.id.add_text_edit_text)
        mInputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mAddTextDoneBtn = view.findViewById(R.id.add_text_btnDone)
        mAddTextCancelBtn = view.findViewById(R.id.add_Text_btnCancel)

        //Setup the color picker for text color
        val addTextColorPickerRecyclerView: RecyclerView =
            view.findViewById(R.id.add_text_color_picker_recycler_view)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        addTextColorPickerRecyclerView.layoutManager = layoutManager
        addTextColorPickerRecyclerView.setHasFixedSize(true)
        val colorPickerAdapter = ColorPickerAdapter(activity)

        //This listener will change the text color when clicked on any color from picker
        colorPickerAdapter.setOnColorPickerClickListener(object : OnColorPickerClickListener {
            override fun onColorPickerClickListener(colorCode: Int) {
                mTextColor  = colorCode
                mAddTextEditText.setTextColor(colorCode)
            }
        })

        addTextColorPickerRecyclerView.adapter = colorPickerAdapter

        //Setup the color picker for background color
        val backgroundColorPickerRecyclerView: RecyclerView = view.findViewById(R.id.add_text_background_color_picker_recycler_view)
        val backgroundColorLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        backgroundColorPickerRecyclerView.layoutManager = backgroundColorLayoutManager
        backgroundColorPickerRecyclerView.setHasFixedSize(true)

        val backgroundColorPickerAdapter = ColorPickerAdapter(activity, true) // Tambahkan flag untuk warna transparan

        backgroundColorPickerAdapter.setOnColorPickerClickListener(object : OnColorPickerClickListener {
            override fun onColorPickerClickListener(colorCode: Int) {
                mBackgroundColor = colorCode
                mAddTextEditText.setBackgroundColor(colorCode)
            }
        })
        backgroundColorPickerRecyclerView.adapter = backgroundColorPickerAdapter

        val sbFontSize: SeekBar = view.findViewById(R.id.sb_font_size)
        sbFontSize.setOnSeekBarChangeListener(this)

        val arguments = requireArguments()

        mAddTextEditText.setText(arguments.getString(EXTRA_INPUT_TEXT))
        mTextColor  = arguments.getInt(EXTRA_COLOR_CODE)
        mBackgroundColor = arguments.getInt(EXTRA_BACKGROUND_COLOR_CODE, Color.TRANSPARENT)
        mTextSize = arguments.getFloat(EXTRA_TEXT_SIZE, 36f)
        mAddTextEditText.setTextColor(mTextColor)
        mAddTextEditText.setBackgroundColor(mBackgroundColor)
        mAddTextEditText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, mTextSize)
        sbFontSize.progress = (mTextSize - MIN_TEXT_SIZE).toInt()
        mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        //Make a callback on activity when user is done with text editing
        mAddTextDoneBtn.setOnClickListener { onClickListenerView ->
            mInputMethodManager.hideSoftInputFromWindow(onClickListenerView.windowToken, 0)
            dismiss()
            val inputText = mAddTextEditText.text.toString()
            val textEditorListener = mTextEditorListener
            if (inputText.isNotEmpty() && textEditorListener != null) {
                textEditorListener.onDone(inputText, mTextColor, mBackgroundColor, mTextSize)
            }
        }
        mAddTextCancelBtn.setOnClickListener{ onClickListenerView ->
            mInputMethodManager.hideSoftInputFromWindow(onClickListenerView.windowToken, 0)
            dismiss()
        }
    }

    //Callback to listener if user is done with text editing
    fun setOnTextEditorListener(textEditorListener: TextEditorListener) {
        mTextEditorListener = textEditorListener
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (seekBar?.id == R.id.sb_font_size) {
            mTextSize = (progress + MIN_TEXT_SIZE).toFloat()
            mAddTextEditText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, mTextSize)
            tooltipTextView.setText(mTextSize.toString())
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    companion object {
        private val TAG: String = TextEditorDialogFragment::class.java.simpleName
        const val EXTRA_INPUT_TEXT = "extra_input_text"
        const val EXTRA_COLOR_CODE = "extra_color_code"
        const val EXTRA_BACKGROUND_COLOR_CODE = "extra_background_color_code"
        const val EXTRA_TEXT_SIZE = "extra_text_size"

        //Show dialog with provide text and text color
        //Show dialog with default text input as empty and text color white
        @JvmOverloads
        fun show(
            appCompatActivity: AppCompatActivity,
            inputText: String = "",
            @ColorInt colorCode: Int = ContextCompat.getColor(appCompatActivity, R.color.white),
            @ColorInt backgroundColor: Int = Color.TRANSPARENT,
            textSize: Float = 36f
        ): TextEditorDialogFragment {
            val args = Bundle()
            args.putString(EXTRA_INPUT_TEXT, inputText)
            args.putInt(EXTRA_COLOR_CODE, colorCode)
            args.putInt(EXTRA_BACKGROUND_COLOR_CODE, backgroundColor)
            args.putFloat(EXTRA_TEXT_SIZE, textSize)
            val fragment = TextEditorDialogFragment()
            fragment.arguments = args
            fragment.show(appCompatActivity.supportFragmentManager, TAG)
            return fragment
        }
    }
}