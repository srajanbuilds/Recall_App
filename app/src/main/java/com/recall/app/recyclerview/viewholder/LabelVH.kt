package com.recall.app.recyclerview.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.recall.app.databinding.RecyclerLabelBinding
import com.recall.app.recyclerview.ItemListener

class LabelVH(private val binding: RecyclerLabelBinding, listener: ItemListener) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            listener.onClick(adapterPosition)
        }

        binding.root.setOnLongClickListener {
            listener.onLongClick(adapterPosition)
            return@setOnLongClickListener true
        }
    }

    fun bind(value: String) {
        binding.root.text = value
    }
}
