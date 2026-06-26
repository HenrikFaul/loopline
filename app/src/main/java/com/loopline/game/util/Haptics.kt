package com.loopline.game.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Thin wrapper for subtle haptic feedback during play. */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var enabled: Boolean = true

    private fun canVibrate() = enabled && vibrator?.hasVibrator() == true

    /** A tiny tick used when a dot is connected or undone. */
    fun tick() {
        if (!canVibrate()) return
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(12L, 70))
        } catch (_: Throwable) {
            // Some OEMs restrict vibration; never crash gameplay over haptics.
        }
    }

    /** A stronger confirmation used on level completion. */
    fun success() {
        if (!canVibrate()) return
        try {
            val timings = longArrayOf(0, 30, 60, 50)
            val amps = intArrayOf(0, 120, 0, 200)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
        } catch (_: Throwable) {
        }
    }
}
