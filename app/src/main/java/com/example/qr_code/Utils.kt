package com.example.qr_code

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.content.Intent

object Utils {
    fun shareImage(context: Context, bitmap: Bitmap) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "shared_image.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 