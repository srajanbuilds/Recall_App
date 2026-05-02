package com.recall.app.fragments

import androidx.lifecycle.LiveData
import com.recall.app.R
import com.recall.app.miscellaneous.Constants
import com.recall.app.room.Item

class DisplayLabel : RecallFragment() {

    override fun getBackground() = R.drawable.label

    override fun getObservable(): LiveData<List<Item>> {
        val label = requireNotNull(requireArguments().getString(Constants.SelectedLabel))
        return model.getNotesByLabel(label)
    }
}
