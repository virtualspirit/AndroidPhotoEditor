package com.virtualspirit.photoediting.filters

import com.virtualspirit.photoeditor.PhotoFilter

interface FilterListener {
    fun onFilterSelected(photoFilter: PhotoFilter)
}