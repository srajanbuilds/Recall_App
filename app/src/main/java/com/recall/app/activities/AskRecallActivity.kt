package com.recall.app.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.recall.app.R
import com.recall.app.core.ai.AiRepository
import com.recall.app.preferences.Preferences
import com.recall.app.room.RecallDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AskRecallActivity : AppCompatActivity() {

    private lateinit var chatHistoryTextView: TextView
    private lateinit var promptEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var btnGetApiKey: MaterialButton
    private lateinit var btnGoToSettings: MaterialButton
    
    private val messages = ArrayList<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ask_recall)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Ask Recall"

        chatHistoryTextView = findViewById(R.id.chatHistoryTextView)
        promptEditText = findViewById(R.id.promptEditText)
        sendButton = findViewById(R.id.sendButton)
        btnGetApiKey = findViewById(R.id.btnGetApiKey)
        btnGoToSettings = findViewById(R.id.btnGoToSettings)

        // Setup guidelines button listeners
        btnGetApiKey.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/"))
            startActivity(intent)
        }

        btnGoToSettings.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_settings", true)
            }
            startActivity(intent)
            finish()
        }

        // Check if Gemini API Key is configured
        val preferences = Preferences.getInstance(application)
        if (preferences.geminiApiKey.isEmpty()) {
            promptEditText.isEnabled = false
            sendButton.isEnabled = false
            promptEditText.hint = "Please set up your API Key first..."
        } else {
            promptEditText.isEnabled = true
            sendButton.isEnabled = true
            promptEditText.hint = "Ask Recall..."
        }

        sendButton.setOnClickListener {
            val query = promptEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                handleQuery(query)
                promptEditText.text.clear()
            }
        }
    }

    private fun handleQuery(query: String) {
        messages.add(Pair("You", query))
        val thinkingIndex = messages.size
        messages.add(Pair("Recall", "Thinking..."))
        renderMessages()
        
        lifecycleScope.launch {
            val db = RecallDatabase.getDatabase(application)
            val allNotes = withContext(Dispatchers.IO) {
                db.getBaseNoteDao().getAllNotes()
            }
            
            // Take the 40 most recent notes
            val sortedNotes = allNotes.sortedByDescending { it.timestamp }.take(40)
            
            val contextString = sortedNotes.joinToString("\n---\n") { 
                "Title: ${it.title}\nBody: ${it.body}" 
            }
            
            val llmPrompt = """
                You are Recall, a helpful AI assistant that answers questions based on the user's notes below.
                
                User's Notes:
                $contextString
                
                User's Question: $query
                
                Answer:
            """.trimIndent()

            val apiKey = Preferences.getInstance(application).geminiApiKey
            val response = AiRepository.generateGeminiResponse(apiKey, llmPrompt)
            
            // Replace "Thinking..." with the actual response
            if (thinkingIndex < messages.size) {
                messages[thinkingIndex] = Pair("Recall", response)
            } else {
                messages.add(Pair("Recall", response))
            }
            renderMessages()
        }
    }

    private fun renderMessages() {
        val sb = StringBuilder()
        for (msg in messages) {
            sb.append("${msg.first}: ${msg.second}\n\n")
        }
        chatHistoryTextView.text = sb.toString().trimEnd()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
