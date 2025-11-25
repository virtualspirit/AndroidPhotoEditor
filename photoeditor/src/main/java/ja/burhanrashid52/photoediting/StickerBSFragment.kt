package ja.burhanrashid52.photoediting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ja.burhanrashid52.photoeditor.R

class StickerBSFragment : BottomSheetDialogFragment() {
    private var mStickerListener: StickerListener? = null
    private var stickerPathList: Array<String> = DEFAULT_STICKER_PATHS
    private var selectionMade: Boolean = false
    
    fun setStickerListener(stickerListener: StickerListener?) {
        mStickerListener = stickerListener
    }
    
    /**
     * Set custom sticker image URLs
     * @param stickerUrls Array of image URLs to display as stickers
     */
    fun setStickerPaths(stickerUrls: Array<String>) {
        stickerPathList = stickerUrls
    }

    interface StickerListener {
        fun onStickerClick(bitmap: Bitmap)
        fun onStickerSelectionCancelled()
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
            mStickerListener?.onStickerSelectionCancelled()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val contentView = View.inflate(context, R.layout.fragment_bottom_sticker_emoji_dialog, null)
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.setBottomSheetCallback(mBottomSheetBehaviorCallback)
        }
        (contentView.parent as View).setBackgroundColor(resources.getColor(android.R.color.transparent))
        val rvEmoji: RecyclerView = contentView.findViewById(R.id.rvEmoji)
        val gridLayoutManager = GridLayoutManager(activity, 3)
        rvEmoji.layoutManager = gridLayoutManager
        val stickerAdapter = StickerAdapter()
        rvEmoji.adapter = stickerAdapter
        rvEmoji.setHasFixedSize(true)
        rvEmoji.setItemViewCacheSize(stickerPathList.size)
    }

    inner class StickerAdapter : RecyclerView.Adapter<StickerAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row_sticker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // Load sticker image from remote url with enhanced configuration
            Glide.with(requireContext())
                    .asBitmap()
                    .load(stickerPathList[position])
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.color.holo_red_light)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onLoadStarted(placeholder: Drawable?) {
                            super.onLoadStarted(placeholder)
                            holder.progressBar.visibility = View.VISIBLE
                        }

                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            holder.progressBar.visibility = View.GONE
                            holder.imgSticker.setImageBitmap(resource)
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            holder.progressBar.visibility = View.GONE
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            holder.imgSticker.setImageDrawable(placeholder)
                        }
                    })
        }

        override fun getItemCount(): Int {
            return stickerPathList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imgSticker: ImageView = itemView.findViewById(R.id.imgSticker)
            val progressBar: android.widget.ProgressBar = itemView.findViewById(R.id.progressBar)

            init {
                itemView.setOnClickListener {
                    if (mStickerListener != null) {
                        Glide.with(requireContext())
                                .asBitmap()
                                .load(stickerPathList[layoutPosition])
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(object : CustomTarget<Bitmap?>(256, 256) {
                                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                                        selectionMade = true
                                        mStickerListener!!.onStickerClick(resource)
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {}
                                })
                    }
                    dismiss()
                }
            }
        }
    }

    companion object {
        // Default Image Urls from flaticon(https://www.flaticon.com/stickers-pack/food-289)
        private val DEFAULT_STICKER_PATHS = arrayOf(
                "https://cdn-icons-png.flaticon.com/256/4392/4392452.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392455.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392459.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392462.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392465.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392467.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392469.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392471.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392522.png",
        )
    }
}
