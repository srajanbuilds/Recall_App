package com.recall.app.core.ai

import android.content.Context
import com.recall.app.room.BaseNote
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object AiRepository {
    private var embeddingEngine: EmbeddingEngine? = null
    private var llamaEngine: LlamaEngine? = null

    fun getEmbeddingEngine(context: Context): EmbeddingEngine {
        if (embeddingEngine == null) {
            val modelFile = ModelDownloader.getEmbeddingModelFile(context)
            embeddingEngine = EmbeddingEngine(modelFile)
        }
        return embeddingEngine!!
    }

    fun getLlamaEngine(context: Context): LlamaEngine {
        if (llamaEngine == null) {
            val modelFile = ModelDownloader.getLlmModelFile(context)
            llamaEngine = LlamaEngine(modelFile).apply { initialize(context) }
        }
        return llamaEngine!!
    }

    /**
     * Compute cosine similarity between two float arrays.
     */
    fun cosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size) return 0f
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        return if (normA == 0f || normB == 0f) 0f else (dotProduct / (Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble()))).toFloat()
    }

    /**
     * Performs a semantic vector search across all provided notes based on a text query.
     */
    fun searchNotes(context: Context, query: String, notes: List<BaseNote>, topK: Int = 3): List<BaseNote> {
        val engine = getEmbeddingEngine(context)
        val queryEmbedding = engine.getEmbedding(query)
        
        return notes
            .filter { it.embedding != null }
            .map { note ->
                val floatBuffer = ByteBuffer.wrap(note.embedding!!).asFloatBuffer()
                val noteEmbedding = FloatArray(floatBuffer.remaining())
                floatBuffer.get(noteEmbedding)
                
                val score = cosineSimilarity(queryEmbedding, noteEmbedding)
                Pair(note, score)
            }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    /**
     * Generates a response using the Google Gemini 1.5 Flash API.
     */
    suspend fun generateGeminiResponse(apiKey: String, prompt: String): String {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }

                connection.outputStream.use { os ->
                    val input = requestJson.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseText)
                    val candidates = responseJson.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).getString("text")
                        }
                    }
                    "Error: Empty response from Gemini API."
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    "Error $responseCode: $errorText"
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            } finally {
                connection?.disconnect()
            }
        }
    }
}
