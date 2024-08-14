package com.rgbstudios.alte.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rgbstudios.alte.R
import com.rgbstudios.alte.data.model.UserDetails
import com.rgbstudios.alte.databinding.ItemVerseBinding
import com.rgbstudios.alte.utils.ToastManager
import com.rgbstudios.alte.viewmodel.AlteViewModel

@SuppressLint("NotifyDataSetChanged")
class AlteVerseAdapter(
    private val context: Context,
    private val viewModel: AlteViewModel,
    private val userClickListener: (UserDetails) -> Unit
) :
    RecyclerView.Adapter<AlteVerseAdapter.AlteViewHolder>() {

    private var list: List<UserDetails> = emptyList()
    private val toastManager = ToastManager()

    // Get the current user's folks list
    private val currentUser = viewModel.currentUser.value
    private val currentUsersFolks = currentUser?.folks.orEmpty()

    fun updateAlteVerseList(alteUsers: List<UserDetails>) {
        list = alteUsers
        notifyDataSetChanged()
    }

    inner class AlteViewHolder(val binding: ItemVerseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlteViewHolder {
        val binding = ItemVerseBinding.inflate(LayoutInflater.from(context), parent, false)
        return AlteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlteViewHolder, position: Int) {
        val user = list[position]
        holder.binding.apply {

            fullNameVerse.text = user.name
            usernameVerse.text = context.getString(R.string.user_name_template, user.username)

            Glide.with(context)
                .asBitmap()
                .load(user.avatarUri)
                .placeholder(R.drawable.user_icon)
                .into(userAvatarVerse)

            setClickListeners(holder, user)

            if (user.uid == currentUser?.uid) {
                handleCurrentUser(holder)
            } else {
                handleNewUser(holder, user)
            }
        }
    }

    private fun setClickListeners(holder: AlteViewHolder, user: UserDetails) {
        holder.binding.apply {
            verseUserLayout.setOnClickListener {
                userClickListener(user)
            }

            connectButton.setOnClickListener {
                if (currentUser != null) {
                    connectProgressBar.visibility = View.VISIBLE
                    invitedTextTV.visibility = View.GONE
                    requestedTextTV.visibility = View.GONE

                    viewModel.sendConnectRequest(currentUser.uid, user.uid) { successful, _ ->
                        if (successful) {
                            toastManager.showShortToast(context, "Connect request sent!")

                            connectProgressBar.visibility = View.GONE
                            connectButton.visibility = View.GONE
                            requestedTextTV.visibility = View.GONE
                            invitedTextTV.visibility = View.VISIBLE
                        } else {
                            toastManager.showShortToast(context, "Something went wrong try again!")

                            connectProgressBar.visibility = View.GONE
                            connectButton.visibility = View.VISIBLE
                            requestedTextTV.visibility = View.GONE
                            invitedTextTV.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun handleCurrentUser(holder: AlteViewHolder) {
        holder.binding.apply {
            folkTV.text = context.getString(R.string.you)
            folkTV.visibility = View.VISIBLE
            connectionControlsLayout.visibility = View.GONE
        }
    }
    private fun handleNewUser(holder: AlteViewHolder, user: UserDetails) {
        holder.binding.apply {
            if (currentUsersFolks.contains(user.uid)) {

                folkTV.text = context.getString(R.string.folk)
                folkTV.visibility = View.VISIBLE
                connectionControlsLayout.visibility = View.GONE
            } else {

                val isRequesting = currentUser?.requests?.contains(user.uid) == true

                val isInvited = currentUser?.invites?.contains(user.uid) == true

                if (isRequesting) {
                    connectProgressBar.visibility = View.GONE
                    connectButton.visibility = View.GONE
                    invitedTextTV.visibility = View.GONE
                    requestedTextTV.visibility = View.VISIBLE
                } else if (isInvited) {
                    connectProgressBar.visibility = View.GONE
                    connectButton.visibility = View.GONE
                    requestedTextTV.visibility = View.GONE
                    invitedTextTV.visibility = View.VISIBLE

                } else {
                    connectProgressBar.visibility = View.GONE
                    invitedTextTV.visibility = View.GONE
                    requestedTextTV.visibility = View.GONE
                    connectButton.visibility = View.VISIBLE

                }
                connectionControlsLayout.visibility = View.VISIBLE
            }
        }
    }


    override fun getItemCount(): Int {
        return list.size
    }

}