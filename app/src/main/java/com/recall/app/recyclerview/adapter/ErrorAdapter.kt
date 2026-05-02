package com.recall.app.recyclerview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.recall.app.databinding.ErrorBinding
import com.recall.app.image.ImageError
import com.recall.app.recyclerview.viewholder.ErrorVH

class ErrorAdapter(private val items: List<ImageError>) : RecyclerView.Adapter<ErrorVH>() {

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ErrorVH, position: Int) {
        val error = items[position]
        holder.bind(error)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErrorVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ErrorBinding.inflate(inflater, parent, false)
        return ErrorVH(binding)
    }
}
