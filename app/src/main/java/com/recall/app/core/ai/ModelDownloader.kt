package com.recall.app.core.ai

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ModelDownloader {

    private const val TAG = "ModelDownloader"

    // Embedding model is open and publicly accessible
    const val EMBEDDING_MODEL_URL =
        "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx"

    // Gemma LLM for MediaPipe — using a public ungated mirror for automatic setup
    const val LLM_MODEL_URL =
        "https://huggingface.co/rperuman/gemma-2b-it-cpu-int4.bin/resolve/main/gemma-2b-it-cpu-int4.bin"

    const val LLM_FILE_NAME = "llm_model.task"
    const val EMBEDDING_FILE_NAME = "embedding_model.onnx"

    // SharedPreferences keys
    private const val PREFS_NAME = "recall_ai_prefs"
    private const val KEY_SETUP_COMPLETE = "ai_setup_complete"

    // ---- Status Checking ----

    fun isSetupComplete(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SETUP_COMPLETE, false)
    }

    fun markSetupComplete(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SETUP_COMPLETE, true)
            .apply()
    }

    fun isEmbeddingModelReady(context: Context): Boolean {
        return getEmbeddingModelFile(context).exists()
    }

    fun isLlmModelReady(context: Context): Boolean {
        return getLlmModelFile(context).exists()
    }

    fun areBothModelsReady(context: Context): Boolean {
        return isEmbeddingModelReady(context) && isLlmModelReady(context)
    }

    // ---- Downloads ----

    suspend fun downloadFile(
        urlStr: String,
        destinationFile: File,
        onProgress: (percent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            var input: InputStream? = null
            var output: OutputStream? = null
            try {
                var url = URL(urlStr)
                connection = url.openConnection() as HttpURLConnection
                connection!!.connectTimeout = 15000
                connection!!.readTimeout = 30000
                connection!!.instanceFollowRedirects = true
                connection!!.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

                var status = connection!!.responseCode
                var redirectCount = 0
                while ((status == HttpURLConnection.HTTP_MOVED_TEMP || 
                        status == HttpURLConnection.HTTP_MOVED_PERM || 
                        status == HttpURLConnection.HTTP_SEE_OTHER ||
                        status == 307 ||
                        status == 308) && redirectCount < 5) {
                    val redirectUrl = connection!!.getHeaderField("Location")
                    url = URL(url, redirectUrl)
                    connection!!.disconnect()
                    connection = url.openConnection() as HttpURLConnection
                    connection!!.connectTimeout = 15000
                    connection!!.readTimeout = 30000
                    connection!!.instanceFollowRedirects = true
                    connection!!.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    status = connection!!.responseCode
                    redirectCount++
                }

                if (status != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP $status: ${connection!!.responseMessage}")
                }

                val fileLength = connection!!.contentLengthLong
                input = connection!!.inputStream
                
                destinationFile.parentFile?.mkdirs()
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                output = FileOutputStream(destinationFile)

                val data = ByteArray(8192) // 8KB buffer
                var total: Long = 0
                var count: Int
                var lastPercent = -1
                
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    output.write(data, 0, count)
                    
                    if (fileLength > 0) {
                        val percent = ((total * 100) / fileLength).toInt()
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent, total, fileLength)
                        }
                    } else {
                        onProgress(-1, total, -1)
                    }
                }
            } finally {
                try { output?.close() } catch (_: Exception) {}
                try { input?.close() } catch (_: Exception) {}
                try { connection?.disconnect() } catch (_: Exception) {}
            }
        }
    }

    fun getLlmModelFile(context: Context): File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), LLM_FILE_NAME)
    }

    fun getEmbeddingModelFile(context: Context): File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), EMBEDDING_FILE_NAME)
    }

    fun isEmbeddingModelInAssets(context: Context): Boolean {
        return try {
            context.assets.open(EMBEDDING_FILE_NAME).use { it.read(ByteArray(1)) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isLlmModelInAssets(context: Context): Boolean {
        return try {
            context.assets.open(LLM_FILE_NAME).use { it.read(ByteArray(1)) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun copyAssetToFile(
        context: Context,
        assetName: String,
        destFile: File,
        onProgress: (percent: Int) -> Unit
    ) {
        try {
            context.assets.open(assetName).use { input ->
                destFile.parentFile?.mkdirs()
                if (destFile.exists()) {
                    destFile.delete()
                }
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(1024 * 1024) // 1 MB buffer
                    
                    var fileSize = 0L
                    try {
                        context.assets.openFd(assetName).use { fd ->
                            fileSize = fd.length
                        }
                    } catch (e: Exception) {
                        fileSize = if (assetName == LLM_FILE_NAME) 1500000000L else 24000000L
                    }

                    var bytesCopied = 0L
                    var read = input.read(buffer)
                    var lastPercent = -1
                    
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        
                        val percent = if (fileSize > 0) ((bytesCopied * 100) / fileSize).toInt() else 0
                        val coercedPercent = percent.coerceIn(0, 100)
                        if (coercedPercent != lastPercent) {
                            lastPercent = coercedPercent
                            onProgress(coercedPercent)
                        }
                        read = input.read(buffer)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset $assetName to ${destFile.absolutePath}", e)
            throw e
        }
    }
}
