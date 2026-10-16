package com.bk.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class BkAccessibilityService : AccessibilityService() {
    companion object { @Volatile var instance: BkAccessibilityService? = null }

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun home() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun back() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun recents() = performGlobalAction(GLOBAL_ACTION_RECENTS)

    /** down = true rola o conteúdo para baixo (dedo sobe). */
    fun scroll(down: Boolean) {
        val m = resources.displayMetrics
        val x = m.widthPixels / 2f
        val (from, to) = if (down) m.heightPixels * 0.7f to m.heightPixels * 0.3f
                         else m.heightPixels * 0.3f to m.heightPixels * 0.7f
        val path = Path().apply { moveTo(x, from); lineTo(x, to) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300)).build(), null, null)
    }
}
