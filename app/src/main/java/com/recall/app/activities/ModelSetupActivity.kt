package com.recall.app.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.recall.app.R
import com.recall.app.core.ai.ModelDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModelSetupActivity : AppCompatActivity() {

    // -- Views --
    private lateinit var progressEmbedding: ProgressBar
    private lateinit var statusEmbedding: TextView
    private lateinit var progressLlm: ProgressBar
    private lateinit var statusLlm: TextView
    private lateinit var cardPrivacy: MaterialCardView
    private lateinit var btnStartDownload: MaterialButton
    private lateinit var btnDone: MaterialButton
    private lateinit var btnSkip: TextView
    private lateinit var subtitleText: TextView

    // -- Copy tracking --
    private var isEmbeddingCopying = false
    private var isLlmCopying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If both models already exist, skip straight through
        if (ModelDownloader.areBothModelsReady(this)) {
            ModelDownloader.markSetupComplete(this)
            launchAskRecall()
            return
        }

        setContentView(R.layout.activity_model_setup)

        // Bind views
        progressEmbedding = findViewById(R.id.progressEmbedding)
        statusEmbedding = findViewById(R.id.statusEmbedding)
        progressLlm = findViewById(R.id.progressLlm)
        statusLlm = findViewById(R.id.statusLlm)
        cardPrivacy = findViewById(R.id.cardPrivacy)
        btnStartDownload = findViewById(R.id.btnStartDownload)
        btnDone = findViewById(R.id.btnDone)
        btnSkip = findViewById(R.id.btnSkip)
        subtitleText = findViewById(R.id.subtitleText)

        // Pre-fill status for models that are already downloaded
        if (ModelDownloader.isEmbeddingModelReady(this)) {
            statusEmbedding.text = "✓ Already downloaded"
            progressEmbedding.progress = 100
        }
        if (ModelDownloader.isLlmModelReady(this)) {
            statusLlm.text = "✓ Already downloaded"
            progressLlm.progress = 100
        }

        // Check if models are in assets
        val isEmbeddingInAssets = ModelDownloader.isEmbeddingModelInAssets(this)
        val isLlmInAssets = ModelDownloader.isLlmModelInAssets(this)

        if (isEmbeddingInAssets || isLlmInAssets) {
            btnStartDownload.text = "Initialize Local AI"
            subtitleText.text = "Recall found local AI models bundled inside the app!\nTap below to initialize them offline."
            
            if (isEmbeddingInAssets && !ModelDownloader.isEmbeddingModelReady(this)) {
                statusEmbedding.text = "Ready to install from app"
            }
            if (isLlmInAssets && !ModelDownloader.isLlmModelReady(this)) {
                statusLlm.text = "Ready to install from app"
            }
        }

        // -- Button listeners --
        btnStartDownload.setOnClickListener { startSetup() }

        btnDone.setOnClickListener {
            ModelDownloader.markSetupComplete(this)
            launchAskRecall()
        }
        btnSkip.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        checkAllDownloadsComplete()
    }

    // =====================================================================
    // Main setup flow
    // =====================================================================

    private fun startSetup() {
        btnStartDownload.isEnabled = false
        
        val isEmbeddingInAssets = ModelDownloader.isEmbeddingModelInAssets(this)
        val isLlmInAssets = ModelDownloader.isLlmModelInAssets(this)
        
        val needsEmbedding = !ModelDownloader.isEmbeddingModelReady(this)
        val needsLlm = !ModelDownloader.isLlmModelReady(this)

        if ((needsEmbedding && isEmbeddingInAssets) || (needsLlm && isLlmInAssets)) {
            btnStartDownload.text = "Initializing…"
        } else {
            btnStartDownload.text = "Downloading…"
        }

        lifecycleScope.launch {
            // 1. Handle Embedding Model
            if (needsEmbedding) {
                progressEmbedding.visibility = View.VISIBLE
                if (isEmbeddingInAssets) {
                    statusEmbedding.text = "Extracting search engine…"
                    isEmbeddingCopying = true
                    val success = withContext(Dispatchers.IO) {
                        try {
                            ModelDownloader.copyAssetToFile(
                                this@ModelSetupActivity,
                                ModelDownloader.EMBEDDING_FILE_NAME,
                                ModelDownloader.getEmbeddingModelFile(this@ModelSetupActivity)
                            ) { percent ->
                                runOnUiThread {
                                    progressEmbedding.progress = percent
                                    statusEmbedding.text = "Extracting search engine… $percent%"
                                }
                            }
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                    isEmbeddingCopying = false
                    if (success) {
                        progressEmbedding.progress = 100
                        statusEmbedding.text = "✓ Ready"
                    } else {
                        statusEmbedding.text = "✗ Failed to extract — downloading fallback..."
                        val downloadSuccess = downloadEmbeddingModelOnline()
                        if (!downloadSuccess) return@launch
                    }
                } else {
                    val downloadSuccess = downloadEmbeddingModelOnline()
                    if (!downloadSuccess) return@launch
                }
            }

            // 2. Handle LLM Model
            if (needsLlm) {
                progressLlm.visibility = View.VISIBLE
                if (isLlmInAssets) {
                    statusLlm.text = "Extracting AI brain…"
                    isLlmCopying = true
                    val success = withContext(Dispatchers.IO) {
                        try {
                            ModelDownloader.copyAssetToFile(
                                this@ModelSetupActivity,
                                ModelDownloader.LLM_FILE_NAME,
                                ModelDownloader.getLlmModelFile(this@ModelSetupActivity)
                            ) { percent ->
                                runOnUiThread {
                                    progressLlm.progress = percent
                                    statusLlm.text = "Extracting AI brain… $percent%"
                                }
                            }
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                    isLlmCopying = false
                    if (success) {
                        progressLlm.progress = 100
                        statusLlm.text = "✓ Ready"
                    } else {
                        statusLlm.text = "✗ Failed to extract — downloading fallback..."
                        val downloadSuccess = downloadLlmModelOnline()
                        if (!downloadSuccess) return@launch
                    }
                } else {
                    val downloadSuccess = downloadLlmModelOnline()
                    if (!downloadSuccess) return@launch
                }
            }

            checkAllDownloadsComplete()
        }
    }

    private suspend fun downloadEmbeddingModelOnline(): Boolean {
        return withContext(Dispatchers.Main) {
            progressEmbedding.visibility = View.VISIBLE
            statusEmbedding.text = "Starting download…"
            
            var errorMsg: String? = null
            val success = try {
                ModelDownloader.downloadFile(
                    ModelDownloader.EMBEDDING_MODEL_URL,
                    ModelDownloader.getEmbeddingModelFile(this@ModelSetupActivity)
                ) { percent, bytesDownloaded, totalBytes ->
                    runOnUiThread {
                        progressEmbedding.progress = percent
                        val mbDown = bytesDownloaded / (1024 * 1024)
                        val mbTotal = totalBytes / (1024 * 1024)
                        statusEmbedding.text = "Downloading… $mbDown MB / $mbTotal MB ($percent%)"
                    }
                }
                true
            } catch (e: Exception) {
                errorMsg = e.message ?: e.localizedMessage
                e.printStackTrace()
                false
            }

            if (success) {
                progressEmbedding.progress = 100
                statusEmbedding.text = "✓ Ready"
                true
            } else {
                statusEmbedding.text = "✗ Download failed: $errorMsg"
                btnStartDownload.isEnabled = true
                btnStartDownload.text = "Retry"
                false
            }
        }
    }

    private suspend fun downloadLlmModelOnline(): Boolean {
        return withContext(Dispatchers.Main) {
            progressLlm.visibility = View.VISIBLE
            statusLlm.text = "Starting download…"
            
            var errorMsg: String? = null
            val success = try {
                ModelDownloader.downloadFile(
                    ModelDownloader.LLM_MODEL_URL,
                    ModelDownloader.getLlmModelFile(this@ModelSetupActivity)
                ) { percent, bytesDownloaded, totalBytes ->
                    runOnUiThread {
                        progressLlm.progress = percent
                        val mbDown = bytesDownloaded / (1024 * 1024)
                        val mbTotal = totalBytes / (1024 * 1024)
                        statusLlm.text = "Downloading… $mbDown MB / $mbTotal MB ($percent%)"
                    }
                }
                true
            } catch (e: Exception) {
                errorMsg = e.message ?: e.localizedMessage
                e.printStackTrace()
                false
            }

            if (success) {
                progressLlm.progress = 100
                statusLlm.text = "✓ Ready"
                true
            } else {
                statusLlm.text = "✗ Download failed: $errorMsg"
                btnStartDownload.isEnabled = true
                btnStartDownload.text = "Retry"
                false
            }
        }
    }

    // =====================================================================
    // Completion
    // =====================================================================

    private fun checkAllDownloadsComplete() {
        if (isEmbeddingCopying || isLlmCopying) return
        
        if (ModelDownloader.areBothModelsReady(this)) {
            cardPrivacy.visibility = View.VISIBLE
            btnDone.visibility = View.VISIBLE
            btnStartDownload.visibility = View.GONE

            subtitleText.text = "Setup complete! Recall AI is ready to use."
            statusEmbedding.text = "✓ Ready"
            statusLlm.text = "✓ Ready"
        }
    }

    private fun launchAskRecall() {
        val intent = Intent(this, AskRecallActivity::class.java)
        startActivity(intent)
        finish()
    }
}
