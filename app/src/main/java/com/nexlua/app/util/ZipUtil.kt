package com.nexlua.app.util

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtil {
    @Throws(IOException::class)
    fun zip(zipFile: File, vararg files: File) {
        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                for (file in files) {
                    zipRecursive(zos, file, file.name) // 直接用 file.name 做 base
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun zipRecursive(zos: ZipOutputStream, file: File, baseName: String) {
        if (file.isDirectory) {
            val children = file.listFiles() ?: return
            for (child in children) {
                val childName = "$baseName/${child.name}"
                zipRecursive(zos, child, childName)
            }
        } else {
            zos.putNextEntry(ZipEntry(baseName))
            FileInputStream(file).use { fis -> fis.copyTo(zos) }
            zos.closeEntry()
        }
    }

    @Throws(IOException::class)
    fun unzip(zipFile: File, destDir: File, includeNames: Array<String>? = null) {
        if (!destDir.exists()) destDir.mkdirs()
        val destPath = destDir.canonicalPath

        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val entryFile = File(destDir, entry.name)
                if (!entryFile.canonicalPath.startsWith(destPath + File.separator)) {
                    throw IOException("Zip entry is trying to escape destination directory: ${entry.name}")
                }

                if (includeNames != null && includeNames.none { entry.name.startsWith(it) }) return@forEach

                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile?.takeIf { !it.exists() }?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(entryFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    fun unzipFromAssets(context: Context, assetFileName: String, outputDir: String) {
        try {
            ZipInputStream(context.assets.open(assetFileName)).use { zipInputStream ->
                var zipEntry: ZipEntry? = zipInputStream.nextEntry
                while (zipEntry != null) {
                    val file = File(outputDir, zipEntry.name)
                    if (!zipEntry.isDirectory) {
                        file.parentFile?.mkdirs()
                    }

                    if (zipEntry.isDirectory) {
                        file.mkdirs()
                    } else {
                        FileOutputStream(file).use { outputStream ->
                            zipInputStream.copyTo(outputStream)
                        }
                    }
                    zipEntry = zipInputStream.nextEntry
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
