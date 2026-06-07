package com.example.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log

class SystemMonitor(private val context: Context) {
    private val TAG = "SystemMonitor"
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    // Holds the state of flashlight
    private var isFlashlightOn = false

    // 1. Get Battery Level & charging state
    fun getBatteryLevel(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            0
        }
    }

    fun isBatteryCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    // 2. Memory Info
    fun getAvailableMemoryGb(): Double {
        return try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            val availGb = memoryInfo.availMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
            String.format("%.2f", availGb).toDouble()
        } catch (e: Exception) {
            0.5
        }
    }

    fun getTotalMemoryGb(): Double {
        return try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            val totalGb = memoryInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
            String.format("%.2f", totalGb).toDouble()
        } catch (e: Exception) {
            4.0
        }
    }

    // 3. Flashlight Control
    fun setFlashlight(state: Boolean): Boolean {
        if (cameraManager == null) return false
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, state)
                isFlashlightOn = state
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flashlight", e)
            false
        }
    }

    fun toggleFlashlight(): Boolean {
        return setFlashlight(!isFlashlightOn)
    }

    fun isFlashlightActive(): Boolean = isFlashlightOn

    // 4. Volume Control
    fun setVolumePercent(percent: Int): Boolean {
        if (audioManager == null) return false
        return try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (maxVolume * percent) / 100
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust volume", e)
            false
        }
    }

    fun getVolumePercent(): Int {
        if (audioManager == null) return 0
        return try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (maxVolume > 0) {
                (currentVolume * 100) / maxVolume
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    // 5. System diagnostics
    fun getHardwareDetails(): String {
        return "Model: ${Build.MODEL}\nManufacturer: ${Build.MANUFACTURER}\nSDK Level: ${Build.VERSION.SDK_INT}\nDevice: ${Build.DEVICE}"
    }
}
