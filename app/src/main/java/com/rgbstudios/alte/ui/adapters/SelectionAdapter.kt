package com.rgbstudios.alte.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.databinding.ItemSelectionBinding
import com.rgbstudios.alte.viewmodel.AlteViewModel

@SuppressLint("NotifyDataSetChanged")
class SelectionAdapter(
    private val clickable: Boolean,
    private val context: Context,
    private val viewModel: AlteViewModel,
    private val itemClickListener: ItemClickListener
) :
    RecyclerView.Adapter<SelectionAdapter.AlteViewHolder>() {

    private var list: List<UserDetails> = emptyList()

    fun updateList(newList: List<UserDetails>) {
        list = newList
        notifyDataSetChanged()
    }

    inner class AlteViewHolder(val binding: ItemSelectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlteViewHolder {
        val binding = ItemSelectionBinding.inflate(LayoutInflater.from(context), parent, false)
        return AlteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlteViewHolder, position: Int) {
        val user = list[position]
        holder.binding.apply {

            userName.text = user.username
            Glide.with(context)
                .asBitmap()
                .load(user.avatarUri)
                .into(imageViewItem)

            if (clickable) {
                selectionItemLayout.setOnClickListener {
                    itemClickListener.onItemClick(user)
                }

                removeItem.visibility = View.VISIBLE
            } else {
                removeItem.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface ItemClickListener {
        fun onItemClick(user: UserDetails)
    }
}