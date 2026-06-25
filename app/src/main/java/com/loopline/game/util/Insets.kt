package com.loopline.game.util

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Enables edge-to-edge rendering (the default on Android 15 / targetSdk 35) and
 * pads [root] by the system-bar and display-cutout insets so the on-screen
 * controls never sit behind the status or navigation bars. Works correctly on
 * older versions too, where the reported insets are zero.
 */
fun Activity.setupEdgeToEdge(root: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        v.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
        WindowInsetsCompat.CONSUMED
    }
}
