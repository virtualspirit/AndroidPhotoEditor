// Buat file baru, misalnya di ja/burhanrashid52/photoeditor/ViewTransform.kt
package ja.burhanrashid52.photoeditor

import android.view.View

data class ViewTransform(
    val translationX: Float,
    val translationY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float
) {
    companion object {
        /**
         * Factory method to capture the current transformation state of a view.
         */
        fun from(view: View): ViewTransform {
            return ViewTransform(
                translationX = view.translationX,
                translationY = view.translationY,
                scaleX = view.scaleX,
                scaleY = view.scaleY,
                rotation = view.rotation
            )
        }
    }

    /**
     * Applies this transformation state to a given view.
     */
    fun applyTo(view: View) {
        view.translationX = this.translationX
        view.translationY = this.translationY
        view.scaleX = this.scaleX
        view.scaleY = this.scaleY
        view.rotation = this.rotation
    }
}