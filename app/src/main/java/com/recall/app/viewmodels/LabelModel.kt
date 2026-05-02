package com.recall.app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.recall.app.room.Label
import com.recall.app.room.RecallDatabase

class LabelModel(app: Application) : AndroidViewModel(app) {

    private val database = RecallDatabase.getDatabase(app)
    private val labelDao = database.getLabelDao()
    val labels = labelDao.getAll()

    fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) =
        executeAsyncWithCallback({ labelDao.insert(label) }, onComplete)
}
