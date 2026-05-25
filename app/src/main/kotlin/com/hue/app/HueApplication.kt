package com.hue.app

import android.app.Application
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.util.Date

@HiltAndroidApp
class HueApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) installCrashFileReporter()
    }

    private fun installCrashFileReporter() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val dir = getExternalFilesDir(null) ?: filesDir
                val file = File(dir, "crash_${System.currentTimeMillis()}.txt")
                file.writeText(buildString {
                    appendLine("=== Hue crash report ===")
                    appendLine("Time   : ${Date()}")
                    appendLine("Thread : ${thread.name}")
                    appendLine("Device : ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    appendLine()
                    appendLine(throwable.stackTraceToString())
                })
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
