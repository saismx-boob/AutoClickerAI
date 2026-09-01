package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.model.HumanizeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AutoClickerAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceBound.value = true
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        _isServiceBound.value = false
        if (instance == this) {
            instance = null
        }
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can inspect active UI nodes for text detection triggers
    }

    override fun onInterrupt() {
        _isServiceBound.value = false
    }

    /**
     * Dispatches a single tap gesture at given coordinate.
     */
    suspend fun performTap(
        x: Float,
        y: Float,
        durationMs: Long = 75L,
        config: HumanizeConfig = HumanizeConfig()
    ): Boolean {
        val target = Humanizer.applyJitter(x, y, config.jitterRadiusPx, config)
        val pressDuration = Humanizer.computePressDuration(durationMs, config)

        val path = Path().apply {
            moveTo(target.x, target.y)
        }

        val stroke = GestureDescription.StrokeDescription(path, 0L, pressDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGestureAsync(gesture)
    }

    /**
     * Dispatches a double tap gesture.
     */
    suspend fun performDoubleTap(
        x: Float,
        y: Float,
        config: HumanizeConfig = HumanizeConfig()
    ): Boolean {
        val tap1 = performTap(x, y, 60L, config)
        if (!tap1) return false
        kotlinx.coroutines.delay(120L)
        return performTap(x, y, 60L, config)
    }

    /**
     * Dispatches a swipe gesture with natural Bezier curve path.
     */
    suspend fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 400L,
        config: HumanizeConfig = HumanizeConfig()
    ): Boolean {
        val path = Humanizer.createNaturalSwipePath(startX, startY, endX, endY, config)
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGestureAsync(gesture)
    }

    /**
     * Inputs text into currently focused editable field or via simulated accessibility text action.
     */
    fun performTextInput(text: String): Boolean {
        if (text.isBlank()) return false
        val rootNode = rootInActiveWindow ?: return false
        val focused = rootNode.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val arguments = android.os.Bundle()
            arguments.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            return focused.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return false
    }

    /**
     * Searches current active window view hierarchy for text matches (OCR-like accessibility inspector).
     */
    fun findTextInActiveWindow(searchText: String): PointF? {
        if (searchText.isBlank()) return null
        val rootNode = rootInActiveWindow ?: return null
        val matchedNodes = rootNode.findAccessibilityNodeInfosByText(searchText)
        if (!matchedNodes.isNullOrEmpty()) {
            val node = matchedNodes.firstOrNull()
            if (node != null) {
                val rect = android.graphics.Rect()
                node.getBoundsInScreen(rect)
                return PointF(rect.exactCenterX(), rect.exactCenterY())
            }
        }
        return null
    }

    private suspend fun dispatchGestureAsync(gesture: GestureDescription): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) continuation.resume(false)
                }
            }
            val dispatched = dispatchGesture(gesture, callback, Handler(Looper.getMainLooper()))
            if (!dispatched && continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    companion object {
        private val _isServiceBound = MutableStateFlow(false)
        val isServiceBound = _isServiceBound.asStateFlow()

        var instance: AutoClickerAccessibilityService? = null
            private set
    }
}
