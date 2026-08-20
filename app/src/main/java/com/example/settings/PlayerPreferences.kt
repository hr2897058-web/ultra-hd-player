package com.example.settings

import android.content.Context
import android.content.SharedPreferences

class PlayerPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ultraplayer_prefs", Context.MODE_PRIVATE)

    var isHardwareAccelerationEnabled: Boolean
        get() = prefs.getBoolean(KEY_HW_ACCEL, true)
        set(value) = prefs.edit().putBoolean(KEY_HW_ACCEL, value).apply()

    var isAuto16kOptimizationEnabled: Boolean
        get() = prefs.getBoolean(KEY_16K_OPTIMIZE, true)
        set(value) = prefs.edit().putBoolean(KEY_16K_OPTIMIZE, value).apply()

    var isShowPerformanceOverlayByDefault: Boolean
        get() = prefs.getBoolean(KEY_PERF_OVERLAY, false)
        set(value) = prefs.edit().putBoolean(KEY_PERF_OVERLAY, value).apply()

    var resumePlayback: Boolean
        get() = prefs.getBoolean(KEY_RESUME_PLAYBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_RESUME_PLAYBACK, value).apply()

    var defaultAspectRatio: String
        get() = prefs.getString(KEY_DEFAULT_ASPECT_RATIO, "FIT") ?: "FIT"
        set(value) = prefs.edit().putString(KEY_DEFAULT_ASPECT_RATIO, value).apply()

    companion object {
        private const val KEY_HW_ACCEL = "key_hw_accel"
        private const val KEY_16K_OPTIMIZE = "key_16k_optimize"
        private const val KEY_PERF_OVERLAY = "key_perf_overlay"
        private const val KEY_RESUME_PLAYBACK = "key_resume_playback"
        private const val KEY_DEFAULT_ASPECT_RATIO = "key_default_aspect_ratio"
    }
}
