package com.recipetracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Keeps photos in the app's private storage. Imported pictures are resized
 * (longest side 2048 px), turned upright and saved as JPEG, so the gallery
 * original can be deleted without losing the photo here.
 */
class PhotoStorage(private val context: Context) {
    val directory: File = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(fileName: String): File = File(directory, fileName)

    /** A temporary file plus a content Uri the camera app can write a new picture into. */
    fun newCameraTarget(): Pair<File, Uri> {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture-${UUID.randomUUID()}.jpg")
        return file to FileProvider.getUriForFile(context, authority(context), file)
    }

    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null

            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val upright = scaleAndRotate(decoded, orientation)
            val name = "${UUID.randomUUID()}.jpg"
            FileOutputStream(file(name)).use { upright.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            if (upright !== decoded) upright.recycle()
            decoded.recycle()
            name
        }.getOrNull()
    }

    /** Saves a bitmap directly (used for imports from backups and in tests). */
    fun writeJpeg(bitmap: Bitmap): String {
        val name = "${UUID.randomUUID()}.jpg"
        FileOutputStream(file(name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return name
    }

    fun delete(fileName: String) {
        file(fileName).delete()
    }

    /**
     * Deletes photo files no recipe refers to any more. Files younger than an hour are
     * kept, because they may belong to a recipe that is still being edited.
     */
    suspend fun deleteUnreferenced(referenced: Set<String>) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
        directory.listFiles()?.forEach { f ->
            if (f.name !in referenced && f.lastModified() < cutoff) f.delete()
        }
        File(context.cacheDir, "camera").listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }

    private fun scaleAndRotate(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        val longest = max(source.width, source.height)
        if (longest > MAX_SIDE) {
            val s = MAX_SIDE.toFloat() / longest
            matrix.postScale(s, s)
        }
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        }
        if (matrix.isIdentity) return source
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    companion object {
        const val MAX_SIDE = 2048

        fun authority(context: Context) = "${context.packageName}.fileprovider"

        /** Decodes a photo no larger than [maxSide] pixels on its longest side. */
        fun decodeScaled(file: File, maxSide: Int): Bitmap? {
            if (!file.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val bmp = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return null
            val longest = max(bmp.width, bmp.height)
            if (longest <= maxSide) return bmp
            val s = maxSide.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(bmp, (bmp.width * s).roundToInt(), (bmp.height * s).roundToInt(), true)
            if (scaled !== bmp) bmp.recycle()
            return scaled
        }
    }
}
