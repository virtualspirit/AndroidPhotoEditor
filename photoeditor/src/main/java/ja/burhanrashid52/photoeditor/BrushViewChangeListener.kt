package ja.burhanrashid52.photoeditor

import ja.burhanrashid52.photoeditor.shape.AbstractShape

/**
 * Created on 1/17/2018.
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 *
 *
 */
interface BrushViewChangeListener {
    fun onViewAdd(drawingView: DrawingView)
    fun onViewRemoved(drawingView: DrawingView)
    fun onStartDrawing()
    fun onStopDrawing()

//    fun onStopDrawing(shape: AbstractShape?)

    fun onShapeCreated(shape: AbstractShape, touchX: Float, touchY: Float)
}