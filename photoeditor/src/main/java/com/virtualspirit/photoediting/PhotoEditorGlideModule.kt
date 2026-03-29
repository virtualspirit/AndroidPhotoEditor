package com.virtualspirit.photoediting

import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.LibraryGlideModule

/**
 * Custom Glide module for PhotoEditor library
 * Configures memory and disk caching for optimal image loading performance
 */
@GlideModule
class PhotoEditorGlideModule : LibraryGlideModule()
