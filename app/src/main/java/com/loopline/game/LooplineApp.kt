package com.loopline.game

import android.app.Application
import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Captures any uncaught exception (on ANY thread) to SharedPreferences so the
 * next launch can surface it. This is how we diagnose device-specific crashes
 * that never reproduce on CI emulators — the user can screenshot/copy the report.
 */
class LooplineApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val report = buildString {
                    append("Loopline crash\n")
                    append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                    append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                    append("Thread: ${thread.name}\n\n")
                    append(sw.toString())
                }
                // commit() (synchronous) — the process is about to die.
                getSharedPreferences("loopline_prefs", MODE_PRIVATE)
                    .edit().putString("last_crash", report).commit()
            } catch (_: Throwable) {
                // never let the reporter itself mask the original crash
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
