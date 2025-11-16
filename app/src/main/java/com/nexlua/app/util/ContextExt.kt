package com.nexlua.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

fun Context.installApk(apkFile: File): Boolean {
    if (!apkFile.exists()) {
        return false
    }
    val intent = Intent(Intent.ACTION_VIEW)
    val uri: Uri
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val authority = "${packageName}.provider"
        uri = FileProvider.getUriForFile(this, authority, apkFile)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        uri = Uri.fromFile(apkFile)
    }
    intent.setDataAndType(uri, "application/vnd.android.package-archive")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
        return true
    } catch (e: Exception) {
        return false
    }
}

fun Context.copyAssetToFile(assetFileName: String, outputFile: File) {
    val assetManager = assets
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null
    try {
        inputStream = assetManager.open(assetFileName)
        outputStream = FileOutputStream(outputFile)

        val buffer = ByteArray(4096)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            inputStream?.close()
            outputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}