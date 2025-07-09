package ja.burhanrashid52.photoediting

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ja.burhanrashid52.photoeditor.R

enum class StrokeStyle {
    SOLID, DASHED, DOTTED
}

class TopPaletteDialogFragment : DialogFragment() {

    private var colorSelectListener: ((Int) -> Unit)? = null
    private var strokeWidthSelectListener: ((Float) -> Unit)? = null
    private var strokeStyleSelectListener: ((StrokeStyle) -> Unit)? = null

    companion object {
        private const val ARG_CURRENT_STROKE_WIDTH = "current_stroke_width"
        const val STROKE_SMALL = 10f
        const val STROKE_MEDIUM = 25f
        const val STROKE_LARGE = 50f

        private const val ARG_CURRENT_STROKE_STYLE = "current_stroke_style"

        fun newInstance(
            currentStrokeWidth: Float?,
            currentStrokeStyle: StrokeStyle? // Parameter baru
        ): TopPaletteDialogFragment {
            val fragment = TopPaletteDialogFragment()
            val args = Bundle()
            currentStrokeWidth?.let { args.putFloat(ARG_CURRENT_STROKE_WIDTH, it) }
            currentStrokeStyle?.let { args.putString(ARG_CURRENT_STROKE_STYLE, it.name) }
            fragment.arguments = args
            return fragment
        }
    }

    fun setOnColorSelectListener(listener: (Int) -> Unit) {
        this.colorSelectListener = listener
    }

    fun setOnStrokeWidthSelectListener(listener: (Float) -> Unit) {
        this.strokeWidthSelectListener = listener
    }

    fun setOnStrokeStyleSelectListener(listener: (StrokeStyle) -> Unit) {
        this.strokeStyleSelectListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_top_palette_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rvColor: RecyclerView = view.findViewById(R.id.rvColors)

        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        rvColor.layoutManager = layoutManager
        rvColor.setHasFixedSize(true)

        // Kita bisa menggunakan kembali ColorPickerAdapter yang sudah ada!
        val colorPickerAdapter = ColorPickerAdapter(requireContext())
        colorPickerAdapter.setOnColorPickerClickListener(object : ColorPickerAdapter.OnColorPickerClickListener {
            override fun onColorPickerClickListener(colorCode: Int) {
                // Panggil listener dan tutup sheet
                colorSelectListener?.invoke(colorCode)
                dismiss()
            }
        })
        rvColor.adapter = colorPickerAdapter

        val rgStrokeWidth: RadioGroup = view.findViewById(R.id.rgStrokeWidth)
        val rbSmall: RadioButton = view.findViewById(R.id.rbStrokeSmall)
        val rbMedium: RadioButton = view.findViewById(R.id.rbStrokeMedium)
        val rbLarge: RadioButton = view.findViewById(R.id.rbStrokeLarge)

        val currentStrokeWidth = arguments?.getFloat(ARG_CURRENT_STROKE_WIDTH) ?: STROKE_MEDIUM

        when (currentStrokeWidth) {
            STROKE_SMALL -> rbSmall.isChecked = true
            STROKE_LARGE -> rbLarge.isChecked = true
            else -> rbMedium.isChecked = true // Default
        }

        rgStrokeWidth.setOnCheckedChangeListener { _, checkedId ->
            val newStrokeWidth = when (checkedId) {
                R.id.rbStrokeSmall -> STROKE_SMALL
                R.id.rbStrokeMedium -> STROKE_MEDIUM
                R.id.rbStrokeLarge -> STROKE_LARGE
                else -> STROKE_MEDIUM
            }
            strokeWidthSelectListener?.invoke(newStrokeWidth)
        }

        val rgStrokeStyle: RadioGroup = view.findViewById(R.id.rgStrokeStyle)
        val rbSolid: RadioButton = view.findViewById(R.id.rbStrokeSolid)
        val rbDashed: RadioButton = view.findViewById(R.id.rbStrokeDashed)
        val rbDotted: RadioButton = view.findViewById(R.id.rbStrokeDotted)

        val currentStyleName = arguments?.getString(ARG_CURRENT_STROKE_STYLE)
        val currentStyle = StrokeStyle.valueOf(currentStyleName ?: StrokeStyle.SOLID.name)

        when (currentStyle) {
            StrokeStyle.DASHED -> rbDashed.isChecked = true
            StrokeStyle.DOTTED -> rbDotted.isChecked = true
            else -> rbSolid.isChecked = true
        }

        rgStrokeStyle.setOnCheckedChangeListener { _, checkedId ->
            val newStyle = when (checkedId) {
                R.id.rbStrokeDashed -> StrokeStyle.DASHED
                R.id.rbStrokeDotted -> StrokeStyle.DOTTED
                else -> StrokeStyle.SOLID
            }
            strokeStyleSelectListener?.invoke(newStyle)
        }
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        window.setGravity(Gravity.TOP)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setDimAmount(0f)
    }
}