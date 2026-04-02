package com.virtualspirit.photoediting

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.virtualspirit.photoeditor.R

enum class StrokeStyle {
    SOLID, DASHED, DOTTED
}

class TopPaletteDialogFragment : DialogFragment() {

    private var colorSelectListener: ((Int) -> Unit)? = null
    private var strokeWidthSelectListener: ((Float) -> Unit)? = null
    private var strokeStyleSelectListener: ((StrokeStyle) -> Unit)? = null
    private var fillColorSelectListener: ((Int?) -> Unit)? = null
    private var dismissListener: (() -> Unit)? = null

    companion object {
        private const val ARG_CURRENT_STROKE_WIDTH = "current_stroke_width"
        const val STROKE_SMALL = 6f
        const val STROKE_MEDIUM = 18f
        const val STROKE_LARGE = 30f

        private const val ARG_CURRENT_STROKE_STYLE = "current_stroke_style"
        private const val ARG_CUSTOM_COLORS = "custom_colors"
        private const val ARG_CURRENT_COLOR = "current_color"
        private const val ARG_IS_CLOSED_SHAPE = "is_closed_shape"
        private const val ARG_CURRENT_FILL_COLOR = "current_fill_color"

        fun newInstance(
            currentStrokeWidth: Float?,
            currentStrokeStyle: StrokeStyle?,
            customColors: IntArray? = null,
            currentColor: Int? = null,
            isClosedShape: Boolean = false,
            currentFillColor: Int? = null
        ): TopPaletteDialogFragment {
            val fragment = TopPaletteDialogFragment()
            val args = Bundle()
            currentStrokeWidth?.let { args.putFloat(ARG_CURRENT_STROKE_WIDTH, it) }
            currentStrokeStyle?.let { args.putString(ARG_CURRENT_STROKE_STYLE, it.name) }
            customColors?.let { args.putIntArray(ARG_CUSTOM_COLORS, it) }
            currentColor?.let { args.putInt(ARG_CURRENT_COLOR, it) }
            args.putBoolean(ARG_IS_CLOSED_SHAPE, isClosedShape)
            currentFillColor?.let { args.putInt(ARG_CURRENT_FILL_COLOR, it) }
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

    fun setOnFillColorSelectListener(listener: (Int?) -> Unit) {
        this.fillColorSelectListener = listener
    }

    fun setOnDismissListener(listener: () -> Unit) {
        this.dismissListener = listener
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        dismissListener?.invoke()
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

        val customColors = arguments?.getIntArray(ARG_CUSTOM_COLORS)
        val baseColorList = if (customColors != null && customColors.isNotEmpty()) {
            customColors.toList()
        } else {
            ColorPickerAdapter.getDefaultColors(requireContext(), false)
        }
        val colorPickerAdapter = ColorPickerAdapter(requireContext(), baseColorList)
        // Pre-select the current shape's color if provided
        arguments?.getInt(ARG_CURRENT_COLOR, Int.MIN_VALUE)
            ?.takeIf { it != Int.MIN_VALUE }
            ?.let { colorPickerAdapter.setSelectedColor(it) }
        colorPickerAdapter.setOnColorPickerClickListener(object : ColorPickerAdapter.OnColorPickerClickListener {
            override fun onColorPickerClickListener(colorCode: Int) {
                colorSelectListener?.invoke(colorCode)
                // No dismiss — stays open so user can draw immediately
            }
        })
        rvColor.adapter = colorPickerAdapter

        // Fill color row — only visible for closed shapes (oval / rect)
        val isClosedShape = arguments?.getBoolean(ARG_IS_CLOSED_SHAPE, false) ?: false
        val tvFillColorLabel: TextView = view.findViewById(R.id.tvFillColorLabel)
        val rvFillColors: RecyclerView = view.findViewById(R.id.rvFillColors)

        if (isClosedShape) {
            tvFillColorLabel.visibility = View.VISIBLE
            rvFillColors.visibility = View.VISIBLE
            rvFillColors.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            rvFillColors.setHasFixedSize(true)

            // Transparent is first = "no fill"
            val fillColorList = listOf(Color.TRANSPARENT) + baseColorList
            val fillColorAdapter = ColorPickerAdapter(requireContext(), fillColorList)
            val currentFillColor = arguments?.getInt(ARG_CURRENT_FILL_COLOR, Int.MIN_VALUE) ?: Int.MIN_VALUE
            if (currentFillColor == Int.MIN_VALUE) {
                fillColorAdapter.setSelectedPosition(0) // no fill selected
            } else {
                fillColorAdapter.setSelectedColor(currentFillColor)
            }
            fillColorAdapter.setOnColorPickerClickListener(object : ColorPickerAdapter.OnColorPickerClickListener {
                override fun onColorPickerClickListener(colorCode: Int) {
                    val fillColor = if (colorCode == Color.TRANSPARENT) null else colorCode
                    fillColorSelectListener?.invoke(fillColor)
                }
            })
            rvFillColors.adapter = fillColorAdapter
        }

        val rgStrokeWidth: RadioGroup = view.findViewById(R.id.rgStrokeWidth)
        val rbSmall: RadioButton = view.findViewById(R.id.rbStrokeSmall)
        val rbMedium: RadioButton = view.findViewById(R.id.rbStrokeMedium)
        val rbLarge: RadioButton = view.findViewById(R.id.rbStrokeLarge)

        val currentStrokeWidth = arguments?.getFloat(ARG_CURRENT_STROKE_WIDTH) ?: STROKE_MEDIUM

        when (currentStrokeWidth) {
            STROKE_SMALL -> rbSmall.isChecked = true
            STROKE_LARGE -> rbLarge.isChecked = true
            else -> rbMedium.isChecked = true
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
