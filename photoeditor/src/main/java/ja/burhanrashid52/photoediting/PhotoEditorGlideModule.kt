package ja.burhanrashid52.photoediting

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

/**
 * Custom Glide module for PhotoEditor library
 * Configures memory and disk caching for optimal image loading performance
 */
@GlideModule
class PhotoEditorGlideModule : AppGlideModule() {
    
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Configure memory cache size (20% of available app memory)
        val calculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(2f)
            .build()
        
        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
        
        // Configure disk cache size (100 MB)
        val diskCacheSizeBytes = 100 * 1024 * 1024L // 100 MB
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes))
        
        // Set default request options
        builder.setDefaultRequestOptions(
            RequestOptions()
                .format(DecodeFormat.PREFER_ARGB_8888) // Better quality
                .disallowHardwareConfig() // Avoid hardware bitmap issues with PhotoEditor
        )
    }
    
    override fun isManifestParsingEnabled(): Boolean {
        // Disable manifest parsing for better performance
        return false
    }
}
