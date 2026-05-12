package com.tapclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlin.random.Random

class ClickAccessibilityService : AccessibilityService() {

    companion object {
        var isRunning = false
        private var isClicking = false
        private var clickX = 0f
        private var clickY = 0f
        private val handler = Handler(Looper.getMainLooper())
        private var clickRunnable: Runnable? = null

        fun setClickPosition(x: Float, y: Float) {
            clickX = x
            clickY = y
        }

        fun startClicking() {
            if (!isRunning || clickX == 0f || clickY == 0f) return
            isClicking = true
            scheduleNextClick()
        }

        fun stopClicking() {
            isClicking = false
            clickRunnable?.let { handler.removeCallbacks(it) }
            clickRunnable = null
        }

        fun isClicking(): Boolean = isClicking

        private fun scheduleNextClick() {
            if (!isClicking) return
            val delay = Random.nextLong(300, 501) // 300~500ms 随机
            clickRunnable = Runnable {
                performClick()
                scheduleNextClick()
            }
            handler.postDelayed(clickRunnable!!, delay)
        }

        private fun performClick() {
            // 实际点击由实例方法执行，这里通过静态引用触发
            instance?.performGestureClick(clickX, clickY)
        }

        private var instance: ClickAccessibilityService? = null

        fun clickNow() {
            instance?.performGestureClick(clickX, clickY)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        instance = this
        Log.d("TapClicker", "AccessibilityService 已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isClicking = false
        instance = null
        clickRunnable?.let { handler.removeCallbacks(it) }
        Log.d("TapClicker", "AccessibilityService 已断开")
    }

    private fun performGestureClick(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1))
            .build()

        dispatchGesture(gesture, null, null)
    }

    fun performServiceClick(x: Float, y: Float) {
        performGestureClick(x, y)
    }
}
