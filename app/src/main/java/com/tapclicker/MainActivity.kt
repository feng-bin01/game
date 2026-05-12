package com.tapclicker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var startBtn: Button
    private val OVERLAY_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startBtn = findViewById(R.id.startFloatingBtn)

        startBtn.setOnClickListener {
            checkPermissionsAndStart()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = ClickAccessibilityService.isRunning
        val isFloatingRunning = FloatingViewService.isRunning

        val sb = StringBuilder("状态：")
        if (isFloatingRunning) sb.append("⚡ 连点运行中")
        else if (hasOverlay && hasAccessibility) sb.append("✅ 就绪")
        else {
            sb.append("⏸ 未就绪")
            if (!hasOverlay) sb.append("\n  - 缺少悬浮窗权限")
            if (!hasAccessibility) sb.append("\n  - 未开启无障碍服务")
        }

        statusText.text = sb.toString()
    }

    private fun checkPermissionsAndStart() {
        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("需要悬浮窗权限")
                .setMessage("TapClicker 需要在其他应用上层显示悬浮球，请授予悬浮窗权限")
                .setPositiveButton("去授予") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
                }
                .setNegativeButton("取消", null)
                .show()
            return
        }

        // 检查无障碍服务
        if (!ClickAccessibilityService.isRunning) {
            AlertDialog.Builder(this)
                .setTitle("需要无障碍服务")
                .setMessage("TapClicker 需要通过无障碍服务模拟点击，请开启 TapClicker 的无障碍开关")
                .setPositiveButton("去开启") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("取消", null)
                .show()
            return
        }

        // 权限都齐了，启动悬浮球
        startFloatingService()
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingViewService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateStatus()
        Toast.makeText(this, "悬浮球已启动", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
    }
}
