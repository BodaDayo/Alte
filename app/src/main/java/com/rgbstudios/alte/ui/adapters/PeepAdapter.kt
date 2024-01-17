package com.rgbstudios.alte.ui.adapters

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.alte.databinding.ItemPeepBinding
import com.rgbstudios.alte.viewmodel.AlteViewModel

class PeepAdapter(private val context: Context, private val viewModel: AlteViewModel) :
    RecyclerView.Adapter<PeepAdapter.AlteViewHolder>() {

    private var list: List<Bitmap> = emptyList()

    fun updatePeeps(peeps: List<Bitmap>) {
        list = peeps
        notifyDataSetChanged()
    }

    inner class AlteViewHolder(val binding: ItemPeepBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlteViewHolder {
        val binding = ItemPeepBinding.inflate(LayoutInflater.from(context), parent, false)
        return AlteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlteViewHolder, position: Int) {
        val peep = list[position]
        holder.binding.apply {
            imagePeep.setImageBitmap(peep)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}