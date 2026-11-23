package com.recipetracker.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.recipetracker.data.PhotoStorage
import java.io.File

/** Opens the Android share sheet for a file (WhatsApp, e-mail, Drive, …). */
fun Context.shareFile(file: File, mimeType: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(this, PhotoStorage.authority(this), file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, chooserTitle))
}

/** Opens a file in another app, e.g. a PDF viewer. */
fun Context.openFile(file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(this, PhotoStorage.authority(this), file)
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        startActivity(view)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}
