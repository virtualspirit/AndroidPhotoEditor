package ja.burhanrashid52.photoeditor

import android.view.View

enum class ActionType {
    ADD, DELETE
}

data class EditorAction(val view: View, val actionType: ActionType)