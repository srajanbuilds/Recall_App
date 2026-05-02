package com.recall.app.recyclerview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.recall.app.databinding.RecyclerLabelBinding
import com.recall.app.recyclerview.ItemListener
import com.recall.app.recyclerview.StringDiffCallback
import com.recall.app.recyclerview.viewholder.LabelVH

class LabelAdapter(private val listener: ItemListener) : ListAdapter<String, LabelVH>(StringDiffCallback()) {

    override fun onBindViewHolder(holder: LabelVH, position: Int) {
        val label = getItem(position)
        holder.bind(label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerLabelBinding.inflate(inflater, parent, false)
        return LabelVH(binding, listener)
    }
}
