package com.hue.app.debug

import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(logFile: File) : Timber.Tree() {

    private val writer: PrintWriter
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        logFile.parentFile?.mkdirs()
        // Rotate when log exceeds 2 MB, keeping one previous file
        if (logFile.exists() && logFile.length() > 2 * 1024 * 1024) {
            val prev = File(logFile.parent, "hue_debug_prev.log")
            prev.delete()
            logFile.renameTo(prev)
        }
        writer = PrintWriter(FileOutputStream(logFile, /* append */ true).bufferedWriter(), /* autoFlush */ true)
    }

    @Synchronized
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG   -> "D"
            Log.INFO    -> "I"
            Log.WARN    -> "W"
            Log.ERROR   -> "E"
            Log.ASSERT  -> "A"
            else        -> "?"
        }
        writer.println("${fmt.format(Date())} $level/$tag: $message")
        t?.let { writer.println(it.stackTraceToString()) }
    }
}
