package com.maximx1.fads.game

import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.io.IOException

/** Storage as files in [directory], one for each name. A file is replaced whole or not at all. */
class FileStorage(
    private val directory: File,
) : Storage {
    override fun read(name: String): String? =
        try {
            AtomicFile(File(directory, name)).readFully().decodeToString()
        } catch (ignored: IOException) {
            null
        }

    override fun write(
        name: String,
        text: String,
    ) {
        val file = AtomicFile(File(directory, name))
        try {
            val stream = file.startWrite()
            try {
                stream.write(text.encodeToByteArray())
                file.finishWrite(stream)
            } catch (e: IOException) {
                file.failWrite(stream)
                throw e
            }
        } catch (e: IOException) {
            Log.w(TAG, "Could not save $name", e)
        }
    }

    private companion object {
        const val TAG = "FileStorage"
    }
}
