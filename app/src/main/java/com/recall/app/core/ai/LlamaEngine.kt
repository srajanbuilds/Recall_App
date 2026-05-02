package com.recall.app.core.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.io.File

class LlamaEngine(private val modelFile: File) {

    private var llmInference: LlmInference? = null
    private var currentFlow: kotlinx.coroutines.channels.ProducerScope<String>? = null

    fun initialize(context: Context) {
        if (!modelFile.exists()) return
        
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512)
                .setTopK(40)
                .setTemperature(0.7f)
                .setResultListener { partialResult, done ->
                    currentFlow?.trySend(partialResult)
                    if (done) {
                        currentFlow?.close()
                        currentFlow = null
                    }
                }
                .build()
                
            llmInference = LlmInference.createFromOptions(context, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Streams the generated text from the LLM model.
     */
    fun generateResponseAsync(prompt: String): Flow<String> = callbackFlow {
        if (llmInference == null) {
            trySend("Error: Model not initialized or downloaded yet.")
            close()
            return@callbackFlow
        }
        
        currentFlow = this
        try {
            llmInference?.generateResponseAsync(prompt)
        } catch (e: Exception) {
            trySend("Error generating response: ${e.message}")
            close()
            currentFlow = null
        }
        
        awaitClose { currentFlow = null }
    }

    fun close() {
        llmInference?.close()
        llmInference = null
    }
}
