package ja.burhanrashid52.photoediting.tools

import android.app.Activity
import android.content.Context
import android.graphics.PorterDuff.Mode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import ja.burhanrashid52.photoeditor.R
import java.util.ArrayList

/**
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.2
 * @since 5/23/2018
 */
class EditingToolsAdapter(private val mOnItemSelected: OnItemSelected) :
    RecyclerView.Adapter<EditingToolsAdapter.ViewHolder>() {
    private val mToolList: MutableList<ToolModel> = ArrayList()
    private var selectedTool: ToolType? = null
    private var previousPosition: Int = RecyclerView.NO_POSITION
    private var currentPosition: Int = 0
    private lateinit var context: Context

    interface OnItemSelected {
        fun onToolSelected(toolType: ToolType)
    }

    internal inner class ToolModel(
        val mToolIcon: Int,
        val mToolType: ToolType
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context;
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_editing_tools, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = mToolList[position]
        holder.imgToolIcon.setImageResource(item.mToolIcon)

        // Logika penyorotan baru
        if (position == currentPosition) {
            // Tool yang sedang aktif
            holder.imgToolIcon.imageTintList = AppCompatResources.getColorStateList(context, R.color.tool_highlight_color)
        } else {
            // Tool tidak aktif
            holder.imgToolIcon.imageTintList = null
        }
    }

    override fun getItemCount(): Int {
        return mToolList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgToolIcon: ImageView = itemView.findViewById(R.id.imgToolIcon)

        init {
            itemView.setOnClickListener { _ ->
                val clickedPosition = layoutPosition
                if (clickedPosition != RecyclerView.NO_POSITION) {
                    val clickedTool = mToolList[clickedPosition].mToolType

                    mOnItemSelected.onToolSelected(clickedTool)

                    val previousPosition = currentPosition
                    currentPosition = clickedPosition

                    notifyItemChanged(previousPosition)
                    notifyItemChanged(currentPosition)
                }
            }
        }
    }

    fun addTool(tool: String) {
        when (tool) {
            "pointer" -> mToolList.add(ToolModel(R.drawable.zl_select, ToolType.POINTER))
            "shape" -> mToolList.add(ToolModel( R.drawable.ic_shape, ToolType.SHAPE))
            "clip" -> mToolList.add(ToolModel(R.drawable.ic_crop, ToolType.CLIP))
            "text" -> mToolList.add(ToolModel(R.drawable.ic_text, ToolType.TEXT))
            "filter" -> mToolList.add(ToolModel(R.drawable.ic_photo_filter, ToolType.FILTER))
            "emoji" -> mToolList.add(ToolModel(R.drawable.ic_insert_emoticon, ToolType.EMOJI))
            "sticker" -> mToolList.add(ToolModel(R.drawable.ic_sticker, ToolType.STICKER))
        }
    }

    fun selectTool(toolToSelect: ToolType) {
        val newPosition = mToolList.indexOfFirst { it.mToolType == toolToSelect }
        if (newPosition != -1) {
            val oldPosition = currentPosition

            currentPosition = newPosition
            selectedTool = toolToSelect

            notifyItemChanged(oldPosition)
            notifyItemChanged(currentPosition)
        }
    }

    init {
        selectedTool = ToolType.POINTER // Set tool default
    }
}