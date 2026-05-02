package com.recall.app.recyclerview.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.recall.app.databinding.RecyclerColorBinding
import com.recall.app.miscellaneous.Operations
import com.recall.app.recyclerview.ItemListener
import com.recall.app.room.Color

class ColorVH(private val binding: RecyclerColorBinding, listener: ItemListener) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            listener.onClick(adapterPosition)
        }
    }

    fun bind(color: Color) {
        val value = Operations.extractColor(color, binding.root.context)
        binding.root.setCardBackgroundColor(value)
    }
}
