package com.rgbstudios.alte.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Chat
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.databinding.ItemLeftSideBinding
import com.rgbstudios.alte.databinding.ItemRightSideBinding
import com.rgbstudios.alte.utils.ImageHandler
import com.rgbstudios.alte.viewmodel.AlteViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("NotifyDataSetChanged")
class ChatAdapter(
    private val context: Context,
    private val currentUserId: String,
    private val viewModel: AlteViewModel,
    private val chatClickListener: ChatClickListener,
) :
    RecyclerView.Adapter<ViewHolder>() {

    private var chatList: List<Chat> = emptyList()
    private var starredList: List<String> = emptyList()
    private val imageHandler = ImageHandler()
    private var currentRecipient: UserDetails? = null
    private var selectedItems = mutableListOf<Int>()
    private var isInSelectMode = false

    fun updateChatList(chats: List<Chat>, recipient: UserDetails?) {
        chatList = chats
        currentRecipient = recipient
        notifyDataSetChanged()
    }

    fun updateStarredList(starredChats: List<String>) {
        starredList = starredChats
        notifyDataSetChanged()
    }

    inner class LeftViewHolder(val binding: ItemLeftSideBinding) :
        ViewHolder(binding.root)

    inner class RightViewHolder(val binding: ItemRightSideBinding) :
        ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LEFT -> {
                val binding =
                    ItemLeftSideBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LeftViewHolder(binding)
            }

            else -> {
                val binding =
                    ItemRightSideBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                RightViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chatList[position]

        if (holder is LeftViewHolder) {
            holder.binding.apply {
                messageTV.text = chat.message

                timeStampTV.text = if (chat.editTimeStamp.isEmpty()) {
                    formatTimeStamp(chat.timeStamp)
                } else {
                    "Edited " + formatTimeStamp(chat.editTimeStamp)
                }

                chatItemLayout.setOnLongClickListener {
                    toggleSelection(position, chat)
                    true
                }

                recipientMessageLayout.setOnLongClickListener {
                    toggleSelection(position, chat)
                    true
                }

                chatImage.setOnLongClickListener {
                    toggleSelection(position, chat)
                    true
                }

                chatItemLayout.setOnClickListener {
                    if (isInSelectMode) {
                        // If in select mode, toggle selection on click
                        toggleSelection(position, chat)
                    }
                }

                recipientMessageLayout.setOnClickListener {
                    if (isInSelectMode) {
                        // If in select mode, toggle selection on click
                        toggleSelection(position, chat)
                    } else {
                        chatClickListener.onReplyClick(position)
                    }
                }

                chatImage.setOnClickListener {
                    if (isInSelectMode) {
                        // If in select mode, toggle selection on click
                        toggleSelection(position, chat)
                    } else {
                        chatClickListener.onImageClick(chat.imageUri)
                    }
                }

                if (chat.recipientTimestamp.isNotEmpty()) {
                    val replied = chatList.find { it.timeStamp == chat.recipientTimestamp }

                    recipientTitleTV.text = replied?.senderId.let {
                        if (it == currentRecipient?.uid) {
                            currentRecipient?.username ?: context.getString(R.string.citizen)
                        } else {
                            context.getString(R.string.you)
                        }
                    }

                    recipientMessage.text = replied?.message.let {
                        if (it != null){
                            if (it.length > 52) {
                                // If the caption is longer than 52 characters, truncate it and add ellipsis
                                it.substring(0, 49) + "..."
                            } else {
                                it
                            }
                        } else ""
                    }

                    recipientMessageLayout.visibility = View.VISIBLE
                } else {
                    recipientMessageLayout.visibility = View.GONE
                }

                if (chat.imageUri.isNotEmpty()) {
                    Glide.with(context).clear(chatImage)

                    chatImage.visibility = View.VISIBLE
                    Glide.with(context)
                        .asBitmap()
                        .load(chat.imageUri)
                        .transform(RoundedCorners(imageHandler.convertDpToPx(16)))
                        .into(chatImage)
                    recipientMessage.minEms = 16
                } else {
                    Glide.with(context).clear(chatImage)

                    chatImage.visibility = View.GONE
                    recipientMessage.minEms = 3
                }

                if (isChatStarred(chat)) {
                    starIV.visibility = View.VISIBLE
                } else {
                    starIV.visibility = View.GONE
                }

                if (isItemSelected(position)) {
                    selectedView.visibility = View.VISIBLE
                } else {
                    selectedView.visibility = View.GONE
                }
            }
        } else if (holder is RightViewHolder) {
            holder.binding.apply {
                messageTV.text = chat.message
                timeStampTV.text = if (chat.editTimeStamp.isEmpty()) {
                    formatTimeStamp(chat.timeStamp)
                } else {
                    "Edited " + formatTimeStamp(chat.editTimeStamp)
                }

                chatItemLayout.setOnLongClickListener {
                    toggleSelection(position, chat)
                    true
                }

                recipientMessageLayout.setOnLongClickListener {
                    toggleSelection(position, chat)
                    true
                }

                chatImage.setOnLongClickListener {
                    toggleSelection(position, chat)
                    true
                }

                chatItemLayout.setOnClickListener {
                    if (isInSelectMode) {
                        // If in select mode, toggle selection on click
                        toggleSelection(position, chat)
                    }
                }

                recipientMessageLayout.setOnClickListener {
                    if (isInSelectMode) {
                        // If in select mode, toggle selection on click
                        toggleSelection(position, chat)
                    } else {
                        chatClickListener.onReplyClick(position)
                    }
                }

                chatImage.setOnClickListener {
                    if (isInSelectMode) {
                        // If in select mode, toggle selection on click
                        toggleSelection(position, chat)
                    } else {
                        chatClickListener.onImageClick(chat.imageUri)
                    }
                }

                if (chat.recipientTimestamp.isNotEmpty()) {
                    val replied = chatList.find { it.timeStamp == chat.recipientTimestamp }

                    recipientTitleTV.text = replied?.senderId.let {
                        if (it == currentRecipient?.uid) {
                            currentRecipient?.username ?: context.getString(R.string.citizen)
                        } else {
                            context.getString(R.string.you)
                        }
                    }

                    recipientMessage.text = replied?.message.let {
                        if (it != null){
                            if (it.length > 52) {
                                // If the caption is longer than 52 characters, truncate it and add ellipsis
                                it.substring(0, 49) + "..."
                            } else {
                                it
                            }
                        } else ""
                    }

                    recipientMessageLayout.visibility = View.VISIBLE
                } else {
                    recipientMessageLayout.visibility = View.GONE
                }

                if (chat.imageUri.isNotEmpty()) {
                    Glide.with(context).clear(chatImage)

                    chatImage.visibility = View.VISIBLE
                    Glide.with(context)
                        .asBitmap()
                        .load(chat.imageUri)
                        .transform(RoundedCorners(imageHandler.convertDpToPx(16)))
                        .into(chatImage)

                    recipientMessage.minEms = 16
                } else {
                    Glide.with(context).clear(chatImage)

                    chatImage.visibility = View.GONE
                    recipientMessage.minEms = 3
                }

                if (chat.isRead) {
                    messageDeliveredIV.setImageResource(R.drawable.double_tick)
                } else {
                    messageDeliveredIV.setImageResource(R.drawable.tick)
                }

                if (isChatStarred(chat)) {
                    starIV.visibility = View.VISIBLE
                } else {
                    starIV.visibility = View.GONE
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
        return chatList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatList[position].senderId == currentUserId) {
            VIEW_TYPE_RIGHT
        } else {
            VIEW_TYPE_LEFT
        }
    }

    private fun isChatStarred(chat: Chat): Boolean {
        val node = chat.timeStamp + chat.senderId
        return starredList.contains(node)
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

        viewModel.updateChatSelection(chat)

        // Set select mode state and update UI accordingly
        isInSelectMode = selectedItems.isNotEmpty()

        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()

        viewModel.clearChatSelection()

        isInSelectMode = selectedItems.isNotEmpty()

        notifyDataSetChanged()
    }

    private fun formatTimeStamp(timeStamp: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = dateFormat.parse(timeStamp)
        val calendar = Calendar.getInstance()

        // Check if the date is today
        if (isSameDay(calendar.time, date)) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return timeFormat.format(date!!)
        }

        // Check if the date is yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        if (isSameDay(calendar.time, date)) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return "Yesterday " + timeFormat.format(date!!)
        }

        // For any other day, format as dd/MM/yy
        val customDateFormat = SimpleDateFormat("dd/MM/yy HH:mm a", Locale.getDefault())
        return customDateFormat.format(date!!)
    }

    private fun isSameDay(date1: Date, date2: Date?): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        if (date2 != null) {
            cal2.time = date2
        }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        private const val VIEW_TYPE_LEFT = 0
        private const val VIEW_TYPE_RIGHT = 1
    }


    interface ChatClickListener {
        fun onReplyClick(position: Int)
        fun onImageClick(imageUri: String)
    }
}
