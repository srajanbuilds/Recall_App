package com.recall.app.miscellaneous

import android.app.Application
import com.recall.app.room.RecallDatabase
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Export {

    fun backupDatabase(app: Application, zipStream: ZipOutputStream) {
        val entry = ZipEntry(RecallDatabase.DatabaseName)
        zipStream.putNextEntry(entry)

        val file = app.getDatabasePath(RecallDatabase.DatabaseName)
        val inputStream = FileInputStream(file)
        inputStream.copyTo(zipStream)
        inputStream.close()

        zipStream.closeEntry()
    }

    fun backupFile(zipStream: ZipOutputStream, root: File?, folder: String, name: String) {
        val file = if (root != null) File(root, name) else null
        if (file != null && file.exists()) {
            val entry = ZipEntry("$folder/$name")
            zipStream.putNextEntry(entry)

            val inputStream = FileInputStream(file)
            inputStream.copyTo(zipStream)
            inputStream.close()

            zipStream.closeEntry()
        }
    }
}
