package com.hue.app.debug

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object LogExportHelper {

    fun logFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "hue_debug.log")

    fun share(context: Context) {
        val file = logFile(context)
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(context, "No debug logs yet.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Hue debug log")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export debug logs"))
    }
}
