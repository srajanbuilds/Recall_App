package com.recall.app.core.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File

class EmbeddingEngine(private val modelFile: File) {

    private val environment = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    init {
        if (modelFile.exists()) {
            session = environment.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
        }
    }

    /**
     * Converts text into a 384-dimensional float vector (embedding).
     * This uses a pre-tokenized approach or relies on onnxruntime-extensions.
     */
    fun getEmbedding(text: String): FloatArray {
        val activeSession = session ?: return FloatArray(384) { 0f } // Return empty vector if no model

        try {
            // Note: A real implementation requires text tokenization into input_ids, attention_mask.
            // For now, if the model has a string input (e.g., using ONNX Runtime Extensions):
            val tensor = OnnxTensor.createTensor(environment, arrayOf(text))
            val inputs = mapOf("input" to tensor)
            
            activeSession.run(inputs).use { result ->
                val outputTensor = result.get(0) as OnnxTensor
                val floatArray = outputTensor.floatBuffer.array()
                return floatArray
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return FloatArray(384) { 0f }
        }
    }

    fun close() {
        session?.close()
        environment.close()
    }
}
