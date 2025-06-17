package ja.burhanrashid52.photoeditor

import android.graphics.Bitmap
import android.view.View
import ja.burhanrashid52.photoediting.StrokeStyle

enum class ActionType {
    ADD, DELETE, TRANSFORM, CHANGE_COLOR, CHANGE_STROKE, CHANGE_STROKE_STYLE, CHANGE_TEXT, CROP, FILTER
}

data class EditorAction(
    val view: View,
    val actionType: ActionType,
    val oldTransform: ViewTransform? = null,
    val newTransform: ViewTransform? = null,
    val oldColor: Int? = null,
    val newColor: Int? = null,
    val oldStrokeWidth: Float? = null,
    val newStrokeWidth: Float? = null,
    val oldStrokeStyle: StrokeStyle? = null,
    val newStrokeStyle: StrokeStyle? = null,
    val oldTextStyle: TextStyleBuilder? = null,
    val newTextStyle: TextStyleBuilder? = null,
    val oldBitmap: Bitmap? = null,
    val newBitmap: Bitmap? = null,
    val oldFilter: PhotoFilter? = null,
    val newFilter: PhotoFilter? = null,
)