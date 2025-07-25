package ja.burhanrashid52.photoediting

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import ja.burhanrashid52.photoediting.ColorPickerAdapter.OnColorPickerClickListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ja.burhanrashid52.photoeditor.R
import ja.burhanrashid52.photoeditor.shape.ShapeType

class ShapeBSFragment : BottomSheetDialogFragment(), SeekBar.OnSeekBarChangeListener {
    private var mProperties: Properties? = null
    private lateinit var mBrushRadioButton: RadioButton
    private lateinit var mLineRadioButton: RadioButton
    private lateinit var mArrowRadioButton: RadioButton
    private lateinit var mRectRadioButton: RadioButton
    private lateinit var mOvalRadioButton: RadioButton


    private var isInteractionReady = false
    private var shapeTools: MutableList<String> = mutableListOf() // arrayOf("draw", "line", "arrow", "square", "circle")

    interface Properties {
        fun onColorChanged(colorCode: Int)
        fun onOpacityChanged(opacity: Int)
        fun onShapeSizeChanged(shapeSize: Int)
        fun onShapePicked(shapeType: ShapeType)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bottom_shapes_dialog, container, false)
    }

    override fun onResume() {
        super.onResume()
        view?.postDelayed({
            isInteractionReady = true
        }, 100)
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isInteractionReady = false // Reset flag setiap kali view dibuat ulang

        val rvColor: RecyclerView = view.findViewById(R.id.shapeColors)
        val sbOpacity = view.findViewById<SeekBar>(R.id.shapeOpacity)
        val sbBrushSize = view.findViewById<SeekBar>(R.id.shapeSize)
        val shapeGroup = view.findViewById<RadioGroup>(R.id.shapeRadioGroup)
        mBrushRadioButton = view.findViewById(R.id.brushRadioButton)
        mLineRadioButton = view.findViewById(R.id.lineRadioButton)
        mArrowRadioButton = view.findViewById(R.id.arrowRadioButton)
        mRectRadioButton = view.findViewById(R.id.rectRadioButton)
        mOvalRadioButton = view.findViewById(R.id.ovalRadioButton)


        shapeGroup.setOnCheckedChangeListener(null)
        shapeGroup.clearCheck()
        
        shapeTools.forEachIndexed { index, tool ->
            when (tool) {
                "draw" -> {
                    mBrushRadioButton.visibility = View.VISIBLE
                    if (index == 0) {
                        mBrushRadioButton.isChecked = true
                    }
                }
                "line" -> {
                    mLineRadioButton.visibility = View.VISIBLE
                    if (index == 0) {
                        mLineRadioButton.isChecked = true
                    }
                }
                "arrow" -> {
                    mArrowRadioButton.visibility = View.VISIBLE
                    if (index == 0) {
                        mArrowRadioButton.isChecked = true
                    }
                }
                "rect" -> {
                    mRectRadioButton.visibility = View.VISIBLE
                    if (index == 0) {
                        mRectRadioButton.isChecked = true
                    }
                }
                "oval" -> {
                    mOvalRadioButton.visibility = View.VISIBLE
                    if (index == 0) {
                        mOvalRadioButton.isChecked = true
                    }
                }
            }
        }

        // shape picker
        shapeGroup.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            Log.d("DrawingView", "masuk setOnCheckedChangeListener")
            if (!isInteractionReady) {
                return@setOnCheckedChangeListener
            }
            when (checkedId) {
                R.id.lineRadioButton -> {
                    mProperties!!.onShapePicked(ShapeType.Line)
                }
                R.id.arrowRadioButton -> {
                    mProperties!!.onShapePicked(ShapeType.Arrow())
                }
                R.id.ovalRadioButton -> {
                    mProperties!!.onShapePicked(ShapeType.Oval)
                }
                R.id.rectRadioButton -> {
                    mProperties!!.onShapePicked(ShapeType.Rectangle)
                }
                R.id.brushRadioButton -> {
                    mProperties!!.onShapePicked(ShapeType.Brush)
                }
                else -> {
                    mProperties!!.onShapePicked(ShapeType.Brush)
                }
            }
        }


        sbOpacity.setOnSeekBarChangeListener(this)
        sbBrushSize.setOnSeekBarChangeListener(this)

        val activity = requireActivity()

        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        rvColor.layoutManager = layoutManager
        rvColor.setHasFixedSize(true)
        val colorPickerAdapter = ColorPickerAdapter(activity, false, 2)
        mProperties!!.onColorChanged(ContextCompat.getColor(activity, R.color.red_color_picker))
        colorPickerAdapter.setOnColorPickerClickListener(object : OnColorPickerClickListener {
            override fun onColorPickerClickListener(colorCode: Int) {
                if (mProperties != null) {
                    mProperties!!.onColorChanged(colorCode)
                }
            }
        })
        rvColor.adapter = colorPickerAdapter
    }

    fun setPropertiesChangeListener(properties: Properties?) {
        mProperties = properties
    }

    override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
        when (seekBar.id) {
            R.id.shapeOpacity -> if (mProperties != null) {
                mProperties!!.onOpacityChanged(i)
            }
            R.id.shapeSize -> if (mProperties != null) {
                mProperties!!.onShapeSizeChanged(i)
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit

    fun addShape(shape: String) {
        shapeTools.add(shape)
    }
}