package ja.burhanrashid52.photoeditor

import android.view.View

enum class ActionType {
    ADD, DELETE, TRANSFORM
}

data class EditorAction(
    val view: View,
    val actionType: ActionType,
    val oldTransform: ViewTransform? = null,
    val newTransform: ViewTransform? = null
)