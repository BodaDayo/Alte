package com.rgbstudios.alte.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.Chat
import com.rgbstudios.alte.databinding.ItemConvoBinding
import com.rgbstudios.alte.viewmodel.AlteViewModel
import com.rgbstudios.alte.data.model.UserDetails
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("NotifyDataSetChanged")
class ConvoAdapter(
    private val context: Context,
    private val viewModel: AlteViewModel,
    private val convoClickListener: ConvoClickListener
) :
    RecyclerView.Adapter<ConvoAdapter.ViewHolder>() {

    private var list: List<Triple<Chat, String, Pair<Int, Int>>> = emptyList()
    private var selectedItems = mutableListOf<Int>()
    private var isInSelectMode = false

    fun updateConvoList(currentChats: List<Triple<Chat, String, Pair<Int, Int>>>) {
        list = currentChats

        clearSelection()
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        val binding: ItemConvoBinding
    ) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConvoBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val convo = list[position]
        holder.binding.apply {

            val receiverUserDetails = viewModel.allUsersList.value?.find { it.uid == convo.second }
            val chat = convo.first
            val counters = convo.third
            val unreadCount = counters.second

            if (receiverUserDetails != null) {
                fullNameTV.text = receiverUserDetails.name
                usernameTV.text =
                    context.getString(R.string.user_name_template, receiverUserDetails.username)

                messageTV.text =
                    chat.message.let { if (it.length > 22) "${it.substring(0, 17)}..." else it }
                timeStampTV.text = formatTimeStamp(chat.timeStamp)

                // Load image with Glide and handle visibility changes
                Glide.with(context)
                    .asBitmap()
                    .load(receiverUserDetails.avatarUri)
                    .placeholder(R.drawable.user_icon)
                    .into(userAvatarMSG)

                convoItemLayout.setOnLongClickListener {
                    toggleSelection(position, receiverUserDetails)
                    true
                }

                convoItemLayout.setOnClickListener {
                    if (isInSelectMode) {
                        // If in select mode, toggle selection on click
                        toggleSelection(position, receiverUserDetails)
                    } else {
                        convoClickListener.onConvoClick(receiverUserDetails)
                    }
                }

                userAvatarMSG.setOnClickListener {
                    if (isInSelectMode) {
                        // If in select mode, toggle selection on click
                        toggleSelection(position, receiverUserDetails)
                    } else {
                        convoClickListener.onAvatarClick(receiverUserDetails)
                    }
                }

                if (chat.senderId != receiverUserDetails.uid) {
                    if (chat.isRead) {
                        messageDeliveredIV.setImageResource(R.drawable.double_tick)
                    } else {
                        messageDeliveredIV.setImageResource(R.drawable.tick)
                    }
                    messageDeliveredIV.visibility = View.VISIBLE
                } else {
                    messageDeliveredIV.visibility = View.GONE
                }

                if (chat.imageUri.isNotEmpty()) {
                    hasImage.visibility = View.VISIBLE
                } else {
                    hasImage.visibility = View.GONE
                }

                root.findViewTreeLifecycleOwner()?.let {

                    // TODO Test this typing status listener out

                    viewModel.recipientTypingStatus.observe(it) { uid ->
                        val yourUid = viewModel.currentUser.value?.uid
                        if (uid == yourUid) {
                            receiverTyping.visibility = View.VISIBLE
                            messageTV.visibility = View.GONE
                        } else {
                            receiverTyping.visibility = View.GONE
                            messageTV.visibility = View.VISIBLE
                        }
                    }
                }

                if (unreadCount > 0) {
                    unreadCounter.text = unreadCount.toString()
                    unreadCounter.visibility = View.VISIBLE
                } else {
                    unreadCounter.visibility = View.GONE
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
        val calendar = Calendar.getInstance()

        // Check if the date is today
        if (isSameDay(calendar.time, date)) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return timeFormat.format(date!!)
        }

        // Check if the date is yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        if (isSameDay(calendar.time, date)) {
            return "Yesterday"
        }

        // For any other day, format as dd/MM/yy
        val customDateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
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


    private fun isItemSelected(position: Int): Boolean {
        return selectedItems.contains(position)
    }

    private fun toggleSelection(position: Int, user: UserDetails) {

        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        viewModel.updateConvoSelection(user)

        // Set select mode state and update UI accordingly
        isInSelectMode = selectedItems.isNotEmpty()

        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()

        viewModel.clearConvoSelection()

        isInSelectMode = selectedItems.isNotEmpty()

        notifyDataSetChanged()
    }

    fun fillSelection() {
        list.forEach {
            val position = list.indexOf(it)

            if (!selectedItems.contains(position)) {
                selectedItems.add(position)

                val user = viewModel.allUsersList.value?.find { uD ->
                    uD.uid == it.second
                }

                if (user != null) {
                    viewModel.updateConvoSelection(user)
                }
            }
        }

        // Set select mode state and update UI accordingly
        isInSelectMode = selectedItems.isNotEmpty()

        notifyDataSetChanged()
    }

    interface ConvoClickListener {
        fun onConvoClick(user: UserDetails)

        fun onAvatarClick(user: UserDetails)
    }

}