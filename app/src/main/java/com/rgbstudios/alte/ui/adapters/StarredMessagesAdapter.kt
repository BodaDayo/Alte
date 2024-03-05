package com.rgbstudios.alte.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Chat
import com.rgbstudios.alte.databinding.ItemStarredMessagesBinding
import com.rgbstudios.alte.utils.ImageHandler
import com.rgbstudios.alte.viewmodel.AlteViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("NotifyDataSetChanged")
class StarredMessagesAdapter(
    private val context: Context,
    private val viewModel: AlteViewModel,
    private val messageClickListener: MessageClickListener
) :
    RecyclerView.Adapter<StarredMessagesAdapter.ViewHolder>() {

    private var list: List<Chat> = emptyList()
    private val imageHandler = ImageHandler()

    private var selectedItems = mutableListOf<Int>()
    private var isInSelectMode = false


    fun updateConvoList(currentChats: List<Chat>) {
        list = currentChats

        clearSelection()
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        val binding: ItemStarredMessagesBinding
    ) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStarredMessagesBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = list[position]
        holder.binding.apply {

            starredLayout.setOnLongClickListener {
                toggleSelection(position, chat)
                true
            }

            starredLayout.setOnClickListener {
                if (isInSelectMode) {
                    // If in select mode, toggle selection on click
                    toggleSelection(position, chat)
                } else {
                    messageClickListener.onMessageClick(chat)
                }
            }

            val senderUserDetails = viewModel.allUsersList.value?.find { it.uid == chat.senderId }
            val receiverUserDetails = viewModel.allUsersList.value?.find { it.uid == chat.receiverId }

            if (senderUserDetails != null && receiverUserDetails != null) {
                senderTV.text = senderUserDetails.username.let {if (it == viewModel.currentUser.value?.username ) "You" else it}
                receiverTV.text = receiverUserDetails.username.let {if (it == viewModel.currentUser.value?.username) "You" else it}

                messageTV.text = chat.message
                timeStampTV.text = formatTimeStamp(chat.timeStamp)

                // Load image with Glide and handle visibility changes
                Glide.with(context)
                    .asBitmap()
                    .load(senderUserDetails.avatarUri)
                    .placeholder(R.drawable.user_icon)
                    .into(userAvatarStar)

                if (chat.imageUri.isNotEmpty()) {
                    Glide.with(context).clear(chatImage)

                    chatImage.visibility = View.VISIBLE
                    Glide.with(context)
                        .asBitmap()
                        .load(chat.imageUri)
                        .transform(RoundedCorners(imageHandler.convertDpToPx(16)))
                        .into(chatImage)
                } else {
                    Glide.with(context).clear(chatImage)

                    chatImage.visibility = View.GONE
                }

                if (isItemSelected(position)) {
                    selectedView.visibility = View.VISIBLE
                } else {
                    selectedView.visibility = View.GONE
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private fun formatTimeStamp(timeStamp: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = dateFormat.parse(timeStamp)

        // For any other day, format as dd/MM/yy
        val customDateFormat = SimpleDateFormat("dd/MM/yy HH:mm a", Locale.getDefault())
        return customDateFormat.format(date!!)
    }

    private fun isItemSelected(position: Int): Boolean {
        return selectedItems.contains(position)
    }

    private fun toggleSelection(position: Int, chat: Chat) {

        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        viewModel.updateStarredSelection(chat)

        // Set select mode state and update UI accordingly
        isInSelectMode = selectedItems.isNotEmpty()

        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()

        viewModel.clearStarredSelection()

        isInSelectMode = selectedItems.isNotEmpty()

        notifyDataSetChanged()
    }


    interface MessageClickListener {
        fun onMessageClick(chat: Chat)
    }

}