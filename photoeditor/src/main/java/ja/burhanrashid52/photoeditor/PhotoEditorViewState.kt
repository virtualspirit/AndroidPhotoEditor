package ja.burhanrashid52.photoeditor

import android.util.Log
import android.view.View
import java.util.*

/**
 * Tracked state of user-added views (stickers, emoji, text, etc)
 */
class PhotoEditorViewState {
    var currentSelectedView: View? = null
    var deleteView: View? = null
    internal val addedViews: MutableList<View> = ArrayList()

    private val undoActions: Stack<EditorAction> = Stack()
    private val redoActions: Stack<EditorAction> = Stack()

    fun clearUndoActions() {
        undoActions.clear()
    }

    fun pushUndoAction(action: EditorAction) {
        undoActions.push(action)
    }

    fun popUndoAction(): EditorAction {
        return undoActions.pop()
    }

    val undoActionsCount: Int
        get() = undoActions.size

    fun clearRedoActions() {
        redoActions.clear()
    }

    fun pushRedoAction(action: EditorAction) {
        redoActions.push(action)
    }

    fun popRedoAction(): EditorAction {
        return redoActions.pop()
    }

    val redoActionsCount: Int
        get() = redoActions.size

    fun clearCurrentSelectedView() {
        currentSelectedView = null
    }

    fun getAddedView(index: Int): View {
        return addedViews[index]
    }

    val addedViewsCount: Int
        get() = addedViews.size

    fun clearAddedViews() {
        addedViews.clear()
    }

    fun addAddedView(view: View) {
        addedViews.add(view)
    }

    fun removeAddedView(view: View) {
        addedViews.remove(view)
    }

    fun removeAddedView(index: Int): View {
        return addedViews.removeAt(index)
    }

    fun containsAddedView(view: View): Boolean {
        return addedViews.contains(view)
    }

    /**
     * Replaces a view in the current "added views" list.
     *
     * @param view The view to replace
     * @return true if the view was found and replaced, false if the view was not found
     */
    fun replaceAddedView(view: View): Boolean {
        val i = addedViews.indexOf(view)
        if (i > -1) {
            addedViews[i] = view
            return true
        }
        return false
    }

    init {
//        addedViews = ArrayList()
//        redoViews = Stack()
    }
}