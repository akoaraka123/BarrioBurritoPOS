package com.example.barrioburritopos.util

import android.content.Context
import android.net.Uri
import java.io.File

object CustomizeImageStorage {

    fun saveImage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "customize_options").apply { mkdirs() }
            val dest = File(dir, "option_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            dest.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
