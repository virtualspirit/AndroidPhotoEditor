package ja.burhanrashid52.photoeditor

import android.util.Log
import android.view.View
import java.util.*

/**
 * Tracked state of user-added views (stickers, emoji, text, etc)
 */
internal class PhotoEditorViewState {
    var currentSelectedView: View? = null
    var deleteView: View? = null
    private val addedViews: MutableList<View> = ArrayList()

    private val undoActions: Stack<EditorAction> = Stack()
    private val redoActions: Stack<EditorAction> = Stack()

    fun clearUndoActions() {
        Log.d("wew", "clearUndoActions")
        undoActions.clear()
    }

    fun pushUndoAction(action: EditorAction) {
        Log.d("wew", "pushUndoAction")
        undoActions.push(action)
    }

    fun popUndoAction(): EditorAction {
        Log.d("wew", "popUndoAction")
        return undoActions.pop()
    }

    val undoActionsCount: Int
        get() = undoActions.size

    fun clearRedoActions() {
        Log.d("wew", "clearRedoActions")
        redoActions.clear()
    }

    fun pushRedoAction(action: EditorAction) {
        Log.d("wew", "pushRedoAction")
        redoActions.push(action)
    }

    fun popRedoAction(): EditorAction {
        Log.d("wew", "popRedoAction")
        return redoActions.pop()
    }

    val redoActionsCount: Int
        get() = redoActions.size

    fun clearCurrentSelectedView() {
        Log.d("wew", "clearCurrentSelectedView")
        currentSelectedView = null
    }

    fun getAddedView(index: Int): View {
        return addedViews[index]
    }

    val addedViewsCount: Int
        get() = addedViews.size

    fun clearAddedViews() {
        Log.d("wew", "clearAddedViews")
        addedViews.clear()
    }

    fun addAddedView(view: View) {
        Log.d("wew", "addAddedView")
        addedViews.add(view)
    }

    fun removeAddedView(view: View) {
        Log.d("wew", "removeAddedView")
        addedViews.remove(view)
    }

    fun removeAddedView(index: Int): View {
        Log.d("wew", "removeAddedView")
        return addedViews.removeAt(index)
    }

    fun containsAddedView(view: View): Boolean {
        Log.d("wew", "containsAddedView")
        return addedViews.contains(view)
    }

    /**
     * Replaces a view in the current "added views" list.
     *
     * @param view The view to replace
     * @return true if the view was found and replaced, false if the view was not found
     */
    fun replaceAddedView(view: View): Boolean {
        Log.d("wew", "replaceAddedView")
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