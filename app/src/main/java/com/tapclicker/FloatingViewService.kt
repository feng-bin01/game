package com.tapclicker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView

class FloatingViewService : Service() {

    companion object {
        var isRunning = false
        private var isClicking = false
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: ImageView
    private var lastClickTime = 0L
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 前台服务通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tapclicker_channel",
                "连点服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)

            val notification = Notification.Builder(this, "tapclicker_channel")
                .setContentTitle("TapClicker")
                .setContentText("连点服务运行中")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .build()
            startForeground(1, notification)
        }

        showFloatingView()
        return START_STICKY
    }

    private fun showFloatingView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        floatingView = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_mylocation)
            setBackgroundColor(0x66FF0000.toInt())
            alpha = 0.8f
            setPadding(20, 20, 20, 20)
        }

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    lastClickTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val now = System.currentTimeMillis()
                    val dt = now - lastClickTime

                    // 检查是否是双击（两次点击间隔 < 300ms）
                    if (dt < 400 && dt > 50) {
                        toggleClicking(params.x.toFloat(), params.y.toFloat())
                    }
                    lastClickTime = now
                    true
                }
                else -> false
            }
        }

        windowManager.addView(floatingView, params)
    }

    private fun toggleClicking(x: Float, y: Float) {
        // 将悬浮球坐标偏移调整为实际屏幕坐标
        ClickAccessibilityService.setClickPosition(x + 60f, y + 60f)

        if (!ClickAccessibilityService.isRunning) return

        if (isClicking) {
            ClickAccessibilityService.stopClicking()
            isClicking = false
            floatingView.setBackgroundColor(0x66FF0000.toInt())
            vibrate()
        } else {
            ClickAccessibilityService.startClicking()
            isClicking = true
            floatingView.setBackgroundColor(0x6600FF00.toInt())
            vibrate()
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        ClickAccessibilityService.stopClicking()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
