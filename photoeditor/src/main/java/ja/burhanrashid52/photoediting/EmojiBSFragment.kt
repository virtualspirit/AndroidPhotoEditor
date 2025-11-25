package ja.burhanrashid52.photoediting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Context
import android.util.Log // Import Log for error handling
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import ja.burhanrashid52.photoeditor.R // Ensure this R import is correct
import java.lang.NumberFormatException
import java.util.ArrayList

class EmojiBSFragment : BottomSheetDialogFragment() {

    private var mEmojiListener: EmojiListener? = null
    private var selectionMade: Boolean = false

    // 1. Declare emojisList as a member variable of the Fragment instance
    // Use lateinit as it will be initialized in setupDialog before use.
    private lateinit var emojisList: ArrayList<String>

    interface EmojiListener {
        fun onEmojiClick(emojiUnicode: String)
        fun onEmojiSelectionCancelled()
    }

    private val mBottomSheetBehaviorCallback: BottomSheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!selectionMade) {
            mEmojiListener?.onEmojiSelectionCancelled()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        // 2. Initialize emojisList here where context is available
        // Use requireContext() to get a non-null context or crash early if not attached
        // Call the static utility function from the companion object
        emojisList = Companion.getEmojis(requireContext())

        // Check if the list loaded correctly (optional but good practice)
        if (emojisList.isEmpty()) {
            Log.e("EmojiBSFragment", "Emoji list failed to load or is empty.")
            // Optionally dismiss or show an error message
            // dismiss()
            // return
        }

        // Use requireContext() or context safely within setupDialog
        val contentView = View.inflate(requireContext(), R.layout.fragment_bottom_sticker_emoji_dialog, null)
        dialog.setContentView(contentView)

        // Rest of your setupDialog code...
        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.setBottomSheetCallback(mBottomSheetBehaviorCallback)
        }
        // Use context safely here too
        (contentView.parent as View).setBackgroundColor(requireContext().resources.getColor(android.R.color.transparent))

        val rvEmoji: RecyclerView = contentView.findViewById(R.id.rvEmoji)
        // Use requireActivity() or requireContext() for the LayoutManager context
        val gridLayoutManager = GridLayoutManager(requireActivity(), 5)
        rvEmoji.layoutManager = gridLayoutManager

        // 3. Create the adapter (it will implicitly use the fragment's emojisList because it's an inner class)
        val emojiAdapter = EmojiAdapter()
        rvEmoji.adapter = emojiAdapter
        rvEmoji.setHasFixedSize(true)
        // Use the initialized list's size
        rvEmoji.setItemViewCacheSize(emojisList.size)
    }

    fun setEmojiListener(emojiListener: EmojiListener?) {
        mEmojiListener = emojiListener
    }

    // 4. The Adapter is an inner class, so it can access the outer fragment's members (emojisList)
    inner class EmojiAdapter : RecyclerView.Adapter<EmojiAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.row_emoji, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // Accesses EmojiBSFragment's emojisList directly
            holder.txtEmoji.text = emojisList[position]
        }

        override fun getItemCount(): Int {
            // Accesses EmojiBSFragment's emojisList directly
            return emojisList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val txtEmoji: TextView = itemView.findViewById(R.id.txtEmoji)

            init {
                itemView.setOnClickListener {
                    // Check adapterPosition to prevent crashes if item is removed/changed
                    // while click is processing
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        if (mEmojiListener != null) {
                            // Accesses EmojiBSFragment's emojisList and mEmojiListener
                            selectionMade = true
                            mEmojiListener?.onEmojiClick(emojisList[adapterPosition])
                        }
                        dismiss() // Dismiss the fragment
                    }
                }
            }
        }
    }

    // 5. Companion object now only holds the static utility functions
    companion object {

        /**
         * Provide the list of emoji in form of unicode string.
         * This is now a utility function.
         *
         * @param context context needed to access resources (non-null)
         * @return list of emoji unicode
         */
        fun getEmojis(context: Context): ArrayList<String> { // Make context non-null
            val convertedEmojiList = ArrayList<String>()
            try {
                // No need for context!! since parameter is non-null
                val emojiList = context.resources.getStringArray(R.array.photo_editor_emoji)
                for (emojiUnicode in emojiList) {
                    convertedEmojiList.add(convertEmoji(emojiUnicode))
                }
            } catch (e: Exception) { // Catch specific exceptions like ResourcesNotFoundException if needed
                Log.e("EmojiBSFragment", "Error loading emoji resources", e)
                // Return empty list on error to prevent crashes downstream
            }
            return convertedEmojiList
        }

        // Consider adding better error logging inside convertEmoji too
        private fun convertEmoji(emoji: String): String {
            return try {
                // U+ prefix is common, ensure substring(2) is correct for your format
                val codePoint = emoji.substring(2).toInt(16)
                String(Character.toChars(codePoint))
            } catch (e: Exception) { // Catch NumberFormatException, IndexOutOfBoundsException
                Log.e("EmojiBSFragment", "Failed to convert emoji string: $emoji", e)
                "" // Return empty string or a placeholder on error
            }
        }
    }
}
