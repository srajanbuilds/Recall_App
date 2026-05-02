package com.recall.app.recyclerview.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.recall.app.databinding.ErrorBinding
import com.recall.app.image.ImageError

class ErrorVH(private val binding: ErrorBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(error: ImageError) {
        binding.Name.text = error.name
        binding.Description.text = error.description
    }
}
