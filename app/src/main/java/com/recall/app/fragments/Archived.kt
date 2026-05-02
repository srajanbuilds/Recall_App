package com.recall.app.fragments

import com.recall.app.R

class Archived : RecallFragment() {

    override fun getBackground() = R.drawable.archive

    override fun getObservable() = model.archivedNotes
}
