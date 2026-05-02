package com.recall.app.recyclerview

interface ItemListener {

    fun onClick(position: Int)

    fun onLongClick(position: Int)
}
