package ja.burhanrashid52.photoediting

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import ja.burhanrashid52.photoeditor.R
import java.util.ArrayList

/**
 * Created by Ahmed Adel on 5/8/17.
 */
class ColorPickerAdapter internal constructor(
    private var context: Context,
    colorPickerColors: List<Int>
) : RecyclerView.Adapter<ColorPickerAdapter.ViewHolder>() {
    private var inflater: LayoutInflater
    private val colorPickerColors: List<Int>
    private lateinit var onColorPickerClickListener: OnColorPickerClickListener

    private var selectedPosition: Int = 0 // RecyclerView.NO_POSITION
    private var addTransparent: Boolean = false

    internal constructor(context: Context) : this(context, getDefaultColors(context, false)) {
        this.context = context
        inflater = LayoutInflater.from(context)
    }

    internal constructor(context: Context, addTransparent: Boolean = false) : this(
        context,
        getDefaultColors(context, addTransparent)
    ) {
        this.context = context
        inflater = LayoutInflater.from(context)
        this.addTransparent = addTransparent
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.color_picker_item_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val borderWidth = if (position == selectedPosition) 10 else 4

        if(this.addTransparent && position == 0) {
            holder.colorPickerView.visibility = View.GONE
            holder.transparentPickerView.visibility = View.VISIBLE
        } else {
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(colorPickerColors[position])
            drawable.setStroke(borderWidth, Color.WHITE)
            holder.colorPickerView.background = drawable
            holder.colorPickerView.visibility = View.VISIBLE
            holder.transparentPickerView.visibility = View.GONE
        }


    }

    override fun getItemCount(): Int {
        return colorPickerColors.size
    }

    fun setOnColorPickerClickListener(onColorPickerClickListener: OnColorPickerClickListener) {
        this.onColorPickerClickListener = onColorPickerClickListener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var colorPickerView: View = itemView.findViewById(R.id.color_picker_view)
        var transparentPickerView: View = itemView.findViewById(R.id.transparent_picker_view)

        init {
            itemView.setOnClickListener {
                onColorPickerClickListener.onColorPickerClickListener(
                    colorPickerColors[adapterPosition]
                )
                val previousSelected = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelected)
                notifyItemChanged(position)
            }
        }
    }

    interface OnColorPickerClickListener {
        fun onColorPickerClickListener(colorCode: Int)
    }

    companion object {
        fun getDefaultColors(context: Context, addTransparent: Boolean): List<Int> {
            val colorPickerColors = ArrayList<Int>()
            if (addTransparent) {
                colorPickerColors.add(Color.TRANSPARENT)
            }
            colorPickerColors.add(ContextCompat.getColor((context), R.color.white))
            colorPickerColors.add(ContextCompat.getColor((context), R.color.black))
            colorPickerColors.add(ContextCompat.getColor((context), R.color.red_color_picker))
            colorPickerColors.add(ContextCompat.getColor((context), R.color.orange_color_picker))
            colorPickerColors.add(ContextCompat.getColor((context), R.color.yellow_color_picker))
            colorPickerColors.add(ContextCompat.getColor((context), R.color.yellow_green_color_picker))
            colorPickerColors.add(
                ContextCompat.getColor(
                    (context),
                    R.color.green_color_picker
                )
            )
            colorPickerColors.add(
                ContextCompat.getColor(
                    (context),
                    R.color.sky_blue_color_picker
                )
            )
            colorPickerColors.add(ContextCompat.getColor((context), R.color.blue_color_picker))
            colorPickerColors.add(ContextCompat.getColor((context), R.color.violet_color_picker))
            colorPickerColors.add(ContextCompat.getColor((context), R.color.grey))
            return colorPickerColors
        }
    }

    init {
        inflater = LayoutInflater.from(context)
        this.colorPickerColors = colorPickerColors
    }
}