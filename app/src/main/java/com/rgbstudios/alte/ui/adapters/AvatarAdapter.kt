package com.rgbstudios.alte.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.alte.databinding.ItemAvatarBinding

class AvatarAdapter(
    private var avatarList: List<Int>,
    private val avatarClickListener: AvatarClickListener,
) :
    RecyclerView.Adapter<AvatarAdapter.ViewHolder>() {

    private var selectedAvatarPosition = -1

    inner class ViewHolder(val binding: ItemAvatarBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val avatarImageResource = avatarList[position]


        holder.binding.apply {

            // Set the avatar image
            defaultAvatarImageView.setImageResource(avatarImageResource)

            defaultAvatarImageView.setOnClickListener {
                avatarClickListener.onAvatarClick(avatarImageResource)
                selectItem(position)
            }

            if (position == selectedAvatarPosition) {
                defaultAvatarImageView.borderWidth = 4
            } else {
                defaultAvatarImageView.borderWidth = 0
            }
        }
    }

    private fun selectItem(position: Int) {
        val previousSelectedColorPosition = selectedAvatarPosition
        selectedAvatarPosition = position
        notifyItemChanged(previousSelectedColorPosition)
        notifyItemChanged(selectedAvatarPosition)
    }

    override fun getItemCount(): Int {
        return avatarList.size
    }

    interface AvatarClickListener {
        fun onAvatarClick(avatar: Int)
    }
}